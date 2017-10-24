package org.stagemonitor.web.servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;
import org.stagemonitor.web.servlet.filter.StatusExposingByteCountingServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

import static junit.framework.Assert.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MonitoredHttpRequestTest {

	private ConfigurationRegistry configuration;
	private io.opentracing.mock.MockTracer tracer;
	private ServletPlugin servletPlugin;

	@Before
	public void setUp() throws Exception {
		configuration = mock(ConfigurationRegistry.class);
		final TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		servletPlugin = spy(new ServletPlugin());
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		when(configuration.getConfig(ServletPlugin.class)).thenReturn(servletPlugin);
		final List<SpanEventListenerFactory> spanEventListenerFactories = new ArrayList<>();
		spanEventListenerFactories.add(new SpanContextInformation.SpanContextSpanEventListener());
		spanEventListenerFactories.add(new MonitoredHttpRequest.HttpSpanEventListener(servletPlugin, tracingPlugin));
		spanEventListenerFactories.add(new SpanContextInformation.SpanFinalizer());
		tracer = new io.opentracing.mock.MockTracer();
		when(tracingPlugin.getTracer()).thenReturn(new SpanWrappingTracer(tracer, spanEventListenerFactories));
		final RequestMonitor requestMonitor = mock(RequestMonitor.class);
		when(tracingPlugin.getRequestMonitor()).thenReturn(requestMonitor);
		assertThat(tracer.scopeManager().active()).isNull();
	}

	@After
	public void tearDown() throws Exception {
		assertThat(tracer.scopeManager().active()).isNull();
		Stagemonitor.getMetric2Registry().removeMatching(Metric2Filter.ALL);
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

		monitoredHttpRequest.createScope().close();
		assertEquals(1, tracer.finishedSpans().size());
		final MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertEquals("/test.js", mockSpan.tags().get(Tags.HTTP_URL.getKey()));
		assertEquals("GET *.js", mockSpan.operationName());
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

		monitoredHttpRequest.createScope().close();
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

		monitoredHttpRequest.createScope().close();
		assertEquals(1, tracer.finishedSpans().size());
		final MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertNull(mockSpan.tags().get("http.referring_site"));
	}

	@Test
	public void testParseUserAgent() throws Exception {
		doReturn(true).when(servletPlugin).isParseUserAgent();
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.js");
		request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

		final MonitoredHttpRequest monitoredHttpRequest = createMonitoredHttpRequest(request);

		monitoredHttpRequest.createScope().close();
		assertEquals(1, tracer.finishedSpans().size());
		final MockSpan mockSpan = tracer.finishedSpans().get(0);
		assertThat(mockSpan.tags()).containsEntry("user_agent.browser", "Chrome");
	}

	private MonitoredHttpRequest createMonitoredHttpRequest(MockHttpServletRequest request) throws IOException {
		return new MonitoredHttpRequest(
				request,
				new StatusExposingByteCountingServletResponse(new MockHttpServletResponse()),
				new MockFilterChain(),
				configuration, Executors.newSingleThreadScheduledExecutor());
	}

	@Test
	public void testGetClientIp() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("10.1.1.3");
		request.addHeader("x-forwarded-for", "10.1.1.1, 10.1.1.2, 10.1.1.3");
		assertThat(MonitoredHttpRequest.getClientIp(request)).isEqualTo("10.1.1.1");
	}
}
