package org.stagemonitor.web.monitor.widget;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

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
import org.stagemonitor.requestmonitor.reporter.RequestTraceReporter;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.StagemonitorSecurityFilter;

public class RequestTraceServletTest {

	private WidgetAjaxRequestTraceReporter reporter;
	private RequestTraceServlet requestTraceServlet;
	private HttpRequestTrace httpRequestTrace;
	private String connectionId;
	private WebPlugin webPlugin;

	@Before
	public void setUp() throws Exception {
		Configuration configuration = mock(Configuration.class);
		RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		webPlugin = mock(WebPlugin.class);

		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(mock(RequestMonitor.class));
		when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(HttpServletRequest.class), any(Configuration.class))).thenReturn(Boolean.TRUE);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		reporter = new WidgetAjaxRequestTraceReporter();
		requestTraceServlet = new RequestTraceServlet(configuration, reporter, 1500);
		requestTraceServlet.init();
		connectionId = UUID.randomUUID().toString();
		httpRequestTrace = new HttpRequestTrace(null, "/test", Collections.emptyMap(), "GET", null, connectionId, true);
		httpRequestTrace.setName("test");
	}

	@Test
	public void testRequestTraceBeforeRequest() throws Exception {
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(httpRequestTrace));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", connectionId);
		MockHttpServletResponse response = new MockHttpServletResponse();

		requestTraceServlet.service(request, response);

		Assert.assertEquals(JsonUtils.toJson(Arrays.asList(httpRequestTrace)), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testTwoRequestTraceBeforeRequest() throws Exception {
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(httpRequestTrace));
		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(httpRequestTrace));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", connectionId);
		MockHttpServletResponse response = new MockHttpServletResponse();

		requestTraceServlet.service(request, response);

		Assert.assertEquals(Arrays.asList(httpRequestTrace.toJson(), httpRequestTrace.toJson()).toString(), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
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
						requestTraceServlet.service(request, response);
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
	public void testRequestTraceAfterRequest() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", connectionId);
		request.setAsyncSupported(false);
		final MockHttpServletResponse response = new MockHttpServletResponse();
		performNonBlockingRequest(request, response);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(httpRequestTrace));
		waitForResponse(response);

		Assert.assertEquals(JsonUtils.toJson(Arrays.asList(httpRequestTrace)), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testRequestTraceAfterRequestDifferentConnection() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", UUID.randomUUID().toString());
		request.setAsyncSupported(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		performNonBlockingRequest(request, response);

		reporter.reportRequestTrace(new RequestTraceReporter.ReportArguments(httpRequestTrace));
		waitForResponse(response);

		Assert.assertEquals("[]", response.getContentAsString());
	}

	@Test
	public void testMissingConnectionId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		MockHttpServletResponse response = new MockHttpServletResponse();

		requestTraceServlet.service(request, response);

		Assert.assertEquals(400, response.getStatus());
	}

	@Test
	public void testInvalidConnectionId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", "");
		MockHttpServletResponse response = new MockHttpServletResponse();

		requestTraceServlet.service(request, response);

		Assert.assertEquals(400, response.getStatus());
	}

	@Test
	public void testWidgetDeactivated() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", "");
		MockHttpServletResponse response = new MockHttpServletResponse();
		Mockito.when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(eq(request), any(Configuration.class))).thenReturn(Boolean.FALSE);

		Configuration configuration = mock(Configuration.class);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		new MockFilterChain(requestTraceServlet, new StagemonitorSecurityFilter(configuration)).doFilter(request, response);

		Assert.assertEquals(404, response.getStatus());
		final HttpRequestTrace requestTrace = mock(HttpRequestTrace.class);
		Assert.assertFalse(reporter.isActive(new RequestTraceReporter.IsActiveArguments(requestTrace)));
	}
}
