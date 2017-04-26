package org.stagemonitor.web.monitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.AbstractSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.requestmonitor.utils.SpanUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonitoredHttpRequestTest {

	private ConfigurationRegistry configuration;
	private String operationName;
	private io.opentracing.mock.MockTracer tracer;

	@Before
	public void setUp() throws Exception {
		configuration = mock(ConfigurationRegistry.class);
		final RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		final WebPlugin webPlugin = new WebPlugin();
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		final List<SpanEventListenerFactory> spanEventListenerFactories = new ArrayList<>();
		spanEventListenerFactories.add(() -> new AbstractSpanEventListener() {
			@Override
			public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
				MonitoredHttpRequestTest.this.operationName = operationName;
			}
		});
		spanEventListenerFactories.add(new MonitoredHttpRequest.HttpSpanEventListener(webPlugin, requestMonitorPlugin, new Metric2Registry()));
		tracer = new io.opentracing.mock.MockTracer();
		when(requestMonitorPlugin.getTracer()).thenReturn(new SpanWrappingTracer(tracer,
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
		assertEquals(1, tracer.finishedSpans().size());
		final MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertEquals("/test.js", mockSpan.tags().get(Tags.HTTP_URL.getKey()));
		assertEquals("GET *.js", operationName);
		assertEquals("GET", mockSpan.tags().get("method"));

		assertEquals("application/json", mockSpan.tags().get(SpanUtils.HTTP_HEADERS_PREFIX + "accept"));
		assertFalse(mockSpan.tags().containsKey(SpanUtils.HTTP_HEADERS_PREFIX + "cookie"));
		assertFalse(mockSpan.tags().containsKey(SpanUtils.HTTP_HEADERS_PREFIX + "Cookie"));

		assertEquals("bar", mockSpan.tags().get(SpanUtils.PARAMETERS_PREFIX + "foo"));
		assertEquals("blubb", mockSpan.tags().get(SpanUtils.PARAMETERS_PREFIX + "bla"));
		assertEquals("XXXX", mockSpan.tags().get(SpanUtils.PARAMETERS_PREFIX + "pwd"));
		assertEquals("XXXX", mockSpan.tags().get(SpanUtils.PARAMETERS_PREFIX + "creditCard"));
		assertFalse(mockSpan.tags().containsKey(Tags.ERROR.getKey()));
	}

	@Test
	public void testReferringSite() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("Referer", "https://www.github.com/stagemonitor/stagemonitor");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		monitoredHttpRequest.createSpan().finish();
		assertEquals(1, tracer.finishedSpans().size());
		final MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertEquals("www.github.com", mockSpan.tags().get("http.referring_site"));
	}

	@Test
	public void testReferringSameHostSite() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("Referer", "https://www.myapp.com:8080/categories");
		request.setServerName("www.myapp.com");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		monitoredHttpRequest.createSpan().finish();
		assertEquals(1, tracer.finishedSpans().size());
		final MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertNull(mockSpan.tags().get("http.referring_site"));
	}

	private MonitoredHttpRequest createMonitoredHttpRequest(MockHttpServletRequest request) throws IOException {
		return new MonitoredHttpRequest(
				request,
				new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
				new MockFilterChain(),
				configuration);
	}
}
