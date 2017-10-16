package org.stagemonitor.web.servlet.widget;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.tracing.GlobalTracerTestHelper;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.reporter.ReportingSpanEventListener;
import org.stagemonitor.tracing.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.util.StringUtils;
import org.stagemonitor.web.servlet.MonitoredHttpRequest;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.filter.StagemonitorSecurityFilter;
import org.stagemonitor.web.servlet.filter.StatusExposingByteCountingServletResponse;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletRequest;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpanServletTest {

	private SpanServlet spanServlet;
	private String connectionId;
	private ServletPlugin servletPlugin;
	private ConfigurationRegistry configuration;
	private Span span;

	@Before
	public void setUp() throws Exception {
		configuration = mock(ConfigurationRegistry.class);

		TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		when(tracingPlugin.getRequestMonitor()).thenReturn(mock(RequestMonitor.class));
		when(tracingPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);

		servletPlugin = mock(ServletPlugin.class);
		when(servletPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(ConfigurationRegistry.class))).thenReturn(Boolean.TRUE);
		when(configuration.getConfig(ServletPlugin.class)).thenReturn(servletPlugin);

		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);

		WidgetAjaxSpanReporter reporter = new WidgetAjaxSpanReporter();
		spanServlet = new SpanServlet(configuration, reporter, 1500);
		spanServlet.init();
		connectionId = UUID.randomUUID().toString();

		final SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor = mock(SamplePriorityDeterminingSpanEventListener.class);
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(anyString(), anyString())).then(invocation -> invocation.getArgument(1));
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(anyString(), anyBoolean())).then(invocation -> invocation.getArgument(1));
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(anyString(), any(Number.class))).then(invocation -> invocation.getArgument(1));
		final ReportingSpanEventListener reportingSpanEventListener = new ReportingSpanEventListener(configuration);
		reportingSpanEventListener.addReporter(reporter);
		Tracer tracer = TracingPlugin.createSpanWrappingTracer(new MockTracer(new ThreadLocalScopeManager(), new B3Propagator()), configuration,
				new Metric2Registry(), ServiceLoader.load(SpanEventListenerFactory.class), samplePriorityDeterminingSpanInterceptor, reportingSpanEventListener);
		GlobalTracerTestHelper.override(tracer);
		when(tracingPlugin.getTracer()).thenReturn(tracer);
	}

	@After
	public void tearDown() throws Exception {
		GlobalTracerTestHelper.resetGlobalTracer();
	}

	private void reportSpan() {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.addHeader(WidgetAjaxSpanReporter.CONNECTION_ID, connectionId);
		final MonitoredHttpRequest monitoredHttpRequest = new MonitoredHttpRequest(request,
				mock(StatusExposingByteCountingServletResponse.class), new MockFilterChain(), configuration, mock(ExecutorService.class));

		span = monitoredHttpRequest.createScope().span();
		span.setOperationName("test");
		span.finish();
	}

	@Test
	public void testSpanBeforeRequest() throws Exception {
		reportSpan();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		request.addParameter("connectionId", connectionId);
		MockHttpServletResponse response = new MockHttpServletResponse();

		spanServlet.service(request, response);

		Assert.assertEquals(spanAsJsonArray(), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testTwoSpanBeforeRequest() throws Exception {
		reportSpan();
		final String span1 = spanAsJson();
		reportSpan();
		final String span2 = spanAsJson();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		request.addParameter("connectionId", connectionId);
		MockHttpServletResponse response = new MockHttpServletResponse();

		spanServlet.service(request, response);

		Assert.assertEquals(Arrays.asList(span1, span2).toString(), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	private String spanAsJsonArray() {
		return Collections.singletonList(spanAsJson()).toString();
	}

	private String spanAsJson() {
		return JsonUtils.toJson(span, SpanUtils.CALL_TREE_ASCII);
	}

	private void performNonBlockingRequest(final HttpServletRequest request, final MockHttpServletResponse response) throws Exception {
		final Object lock = new Object();
		synchronized (lock) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						synchronized (lock) {
							lock.notifyAll();
						}
						spanServlet.service(request, response);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
			lock.wait();
		}
//		Thread.sleep(100);
	}

	private void waitForResponse(MockHttpServletResponse response) throws UnsupportedEncodingException, InterruptedException {
		final int maxWait = 2_000;
		int wait = 0;
		while (StringUtils.isEmpty(response.getContentAsString()) && wait <= maxWait) {
			wait += 10;
			Thread.sleep(10);
		}
	}

	@Test
	public void testSpanAfterRequest() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		request.addParameter("connectionId", connectionId);
		request.setAsyncSupported(false);
		final MockHttpServletResponse response = new MockHttpServletResponse();
		performNonBlockingRequest(request, response);

		reportSpan();
		waitForResponse(response);

		Assert.assertEquals(spanAsJsonArray(), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testSpanAfterRequestDifferentConnection() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		request.addParameter("connectionId", UUID.randomUUID().toString());
		request.setAsyncSupported(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		performNonBlockingRequest(request, response);

		reportSpan();
		waitForResponse(response);

		Assert.assertEquals("[]", response.getContentAsString());
	}

	@Test
	public void testMissingConnectionId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		MockHttpServletResponse response = new MockHttpServletResponse();

		spanServlet.service(request, response);

		Assert.assertEquals(400, response.getStatus());
	}

	@Test
	public void testInvalidConnectionId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		request.addParameter("connectionId", "");
		MockHttpServletResponse response = new MockHttpServletResponse();

		spanServlet.service(request, response);

		Assert.assertEquals(400, response.getStatus());
	}

	@Test
	public void testWidgetDeactivated() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		request.addParameter("connectionId", "");
		MockHttpServletResponse response = new MockHttpServletResponse();
		Mockito.when(servletPlugin.isWidgetAndStagemonitorEndpointsAllowed(eq(request), any(ConfigurationRegistry.class))).thenReturn(Boolean.FALSE);

		ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
		when(configuration.getConfig(ServletPlugin.class)).thenReturn(servletPlugin);
		new MockFilterChain(spanServlet, new StagemonitorSecurityFilter(configuration)).doFilter(request, response);

		Assert.assertEquals(404, response.getStatus());
	}
}
