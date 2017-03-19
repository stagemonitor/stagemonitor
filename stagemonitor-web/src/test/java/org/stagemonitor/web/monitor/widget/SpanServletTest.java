package org.stagemonitor.web.monitor.widget;

import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.reporter.ElasticsearchSpanReporter;
import org.stagemonitor.requestmonitor.tracing.jaeger.SpanJsonModule;
import org.stagemonitor.requestmonitor.utils.SpanUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.filter.StagemonitorSecurityFilter;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import io.opentracing.Span;

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

	@Before
	public void setUp() throws Exception {
		JsonUtils.getMapper().registerModule(new SpanJsonModule());
		Configuration configuration = mock(Configuration.class);
		RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		webPlugin = mock(WebPlugin.class);

		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(mock(RequestMonitor.class));
		when(requestMonitorPlugin.getTracer()).thenReturn(new com.uber.jaeger.Tracer.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true)).build());
		when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(Configuration.class))).thenReturn(Boolean.TRUE);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		reporter = new WidgetAjaxSpanReporter();
		spanServlet = new SpanServlet(configuration, reporter, 1500);
		spanServlet.init();
		connectionId = UUID.randomUUID().toString();
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.addHeader(WidgetAjaxSpanReporter.CONNECTION_ID, connectionId);
		final MonitoredHttpRequest monitoredHttpRequest = new MonitoredHttpRequest(request, mock(StatusExposingByteCountingServletResponse.class), new MockFilterChain(), configuration);
		span = monitoredHttpRequest.createSpan();
		spanContext = SpanContextInformation.forSpan(span);
		span.setOperationName("test");
		spanContext.setSpan(span);

		// init jackson module
		new ElasticsearchSpanReporter();
	}

	@Test
	public void testSpanBeforeRequest() throws Exception {
		reporter.report(spanContext);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		request.addParameter("connectionId", connectionId);
		MockHttpServletResponse response = new MockHttpServletResponse();

		spanServlet.service(request, response);

		Assert.assertEquals(spanAsJsonArray(), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testTwoSpanBeforeRequest() throws Exception {
		reporter.report(spanContext);
		reporter.report(spanContext);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/spans");
		request.addParameter("connectionId", connectionId);
		MockHttpServletResponse response = new MockHttpServletResponse();

		spanServlet.service(request, response);

		Assert.assertEquals(Arrays.asList(spanAsJson(), spanAsJson()).toString(), response.getContentAsString());
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
		while (StringUtils.isEmpty(response.getContentAsString())) {
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

		reporter.report(spanContext);
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

		reporter.report(spanContext);
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
		Mockito.when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(eq(request), any(Configuration.class))).thenReturn(Boolean.FALSE);

		Configuration configuration = mock(Configuration.class);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		new MockFilterChain(spanServlet, new StagemonitorSecurityFilter(configuration)).doFilter(request, response);

		Assert.assertEquals(404, response.getStatus());
		Assert.assertFalse(reporter.isActive(SpanContextInformation.forUnitTest(mock(Span.class))));
	}
}
