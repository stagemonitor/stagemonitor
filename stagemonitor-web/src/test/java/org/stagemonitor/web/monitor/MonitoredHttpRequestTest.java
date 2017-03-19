package org.stagemonitor.web.monitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.MockTracer;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.TagRecordingSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.AbstractSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.requestmonitor.utils.SpanUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonitoredHttpRequestTest {

	private Configuration configuration;
	private Map<String, Object> tags = new HashMap<>();
	private String operationName;

	@Before
	public void setUp() throws Exception {
		configuration = mock(Configuration.class);
		final RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		final WebPlugin webPlugin = new WebPlugin();
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		final List<SpanEventListenerFactory> spanEventListenerFactories = TagRecordingSpanEventListener.asList(tags);
		spanEventListenerFactories.add(() -> new AbstractSpanEventListener() {
			@Override
			public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
				MonitoredHttpRequestTest.this.operationName = operationName;
			}
		});
		spanEventListenerFactories.add(new MonitoredHttpRequest.HttpSpanEventListener(webPlugin, requestMonitorPlugin, new Metric2Registry()));
		when(requestMonitorPlugin.getTracer()).thenReturn(new SpanWrappingTracer(new MockTracer(),
				spanEventListenerFactories));
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(requestMonitor);
	}

	@After
	public void tearDown() throws Exception {
		Stagemonitor.reset();
	}

	@Test
	public void testGetRequestName() throws Exception {
		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(new MockHttpServletRequest("GET", "/test.js"));
		assertEquals("GET *.js", monitoredHttpRequest.getRequestName());
	}

	@Test
	public void testCreateSpan() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addParameter("foo", "bar");
		request.addParameter("bla", "blubb");
		request.addParameter("pwd", "secret");
		request.addParameter("creditCard", "123456789");
		request.addHeader("Cookie", "foobar");
		request.addHeader("Accept", "application/json");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		final Span span = monitoredHttpRequest.createSpan();
		span.finish();
		assertEquals("/test.js", tags.get(Tags.HTTP_URL.getKey()));
		assertEquals("GET *.js", operationName);
		assertEquals("GET", tags.get("method"));

		assertEquals("application/json", tags.get(SpanUtils.HTTP_HEADERS_PREFIX + "accept"));
		assertFalse(tags.containsKey(SpanUtils.HTTP_HEADERS_PREFIX + "cookie"));
		assertFalse(tags.containsKey(SpanUtils.HTTP_HEADERS_PREFIX + "Cookie"));

		assertEquals("bar", tags.get(SpanUtils.PARAMETERS_PREFIX + "foo"));
		assertEquals("blubb", tags.get(SpanUtils.PARAMETERS_PREFIX + "bla"));
		assertEquals("XXXX", tags.get(SpanUtils.PARAMETERS_PREFIX + "pwd"));
		assertEquals("XXXX", tags.get(SpanUtils.PARAMETERS_PREFIX + "creditCard"));
	}

	@Test
	public void testReferringSite() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("Referer", "https://www.github.com/stagemonitor/stagemonitor");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		monitoredHttpRequest.createSpan();
		assertEquals("www.github.com", tags.get("http.referring_site"));
	}

	@Test
	public void testReferringSameHostSite() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("Referer", "https://www.myapp.com:8080/categories");
		request.setServerName("www.myapp.com");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		monitoredHttpRequest.createSpan();
		assertNull(tags.get("http.referring_site"));
	}

	private MonitoredHttpRequest createMonitoredHttpRequest(MockHttpServletRequest request) throws IOException {
		return new MonitoredHttpRequest(
				request,
				new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
				new MockFilterChain(),
				configuration);
	}
}
