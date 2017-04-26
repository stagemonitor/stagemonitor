package org.stagemonitor.web.monitor.widget;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchSpanReporter;
import org.stagemonitor.requestmonitor.reporter.ReadbackSpan;
import org.stagemonitor.requestmonitor.reporter.ReportingSpanEventListener;
import org.stagemonitor.requestmonitor.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.B3Propagator;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.requestmonitor.utils.SpanUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.filter.StagemonitorSecurityFilter;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpanServletTest {

	private WidgetAjaxSpanReporter reporter;
	private SpanServlet spanServlet;
	private String connectionId;
	private WebPlugin webPlugin;
	private Span span;
	private SpanContextInformation spanContext;
	private ConfigurationRegistry configuration;

	@Before
	public void setUp() throws Exception {
		JsonUtils.getMapper().registerModule(new ReadbackSpan.SpanJsonModule());
		configuration = mock(ConfigurationRegistry.class);

		RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(mock(RequestMonitor.class));
		when(requestMonitorPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);

		webPlugin = mock(WebPlugin.class);
		when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(ConfigurationRegistry.class))).thenReturn(Boolean.TRUE);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);

		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);

		reporter = new WidgetAjaxSpanReporter();
		spanServlet = new SpanServlet(configuration, reporter, 1500);
		spanServlet.init();
		connectionId = UUID.randomUUID().toString();

		final SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor = mock(SamplePriorityDeterminingSpanEventListener.class);
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(anyString(), anyString())).then(invocation -> invocation.getArgument(1));
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(anyString(), anyBoolean())).then(invocation -> invocation.getArgument(1));
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(anyString(), any(Number.class))).then(invocation -> invocation.getArgument(1));
		final ReportingSpanEventListener reportingSpanEventListener = new ReportingSpanEventListener(configuration);
		reportingSpanEventListener.addReporter(reporter);
		Tracer tracer = RequestMonitorPlugin.createSpanWrappingTracer(new MockTracer(new B3Propagator()), configuration,
				new Metric2Registry(), ServiceLoader.load(SpanEventListenerFactory.class), samplePriorityDeterminingSpanInterceptor, reportingSpanEventListener);
		when(requestMonitorPlugin.getTracer()).thenReturn(tracer);

		// init jackson module
		new ElasticsearchSpanReporter();
	}

	private void reportSpan() {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.addHeader(WidgetAjaxSpanReporter.CONNECTION_ID, connectionId);
		final MonitoredHttpRequest monitoredHttpRequest = new MonitoredHttpRequest(request,
				mock(StatusExposingByteCountingServletResponse.class), new MockFilterChain(), configuration);

		span = monitoredHttpRequest.createSpan();
		spanContext = SpanContextInformation.forSpan(span);
		span.setOperationName("test");
		spanContext.setSpan(span);
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
		return JsonUtils.toJson(spanContext.getReadbackSpan(), SpanUtils.CALL_TREE_ASCII);
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
		Mockito.when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(eq(request), any(ConfigurationRegistry.class))).thenReturn(Boolean.FALSE);

		ConfigurationRegistry configuration = mock(ConfigurationRegistry.class);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		new MockFilterChain(spanServlet, new StagemonitorSecurityFilter(configuration)).doFilter(request, response);

		Assert.assertEquals(404, response.getStatus());
		Assert.assertFalse(reporter.isActive(SpanContextInformation.forUnitTest(mock(Span.class))));
	}
}
