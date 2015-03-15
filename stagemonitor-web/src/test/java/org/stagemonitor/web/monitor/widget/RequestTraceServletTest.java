package org.stagemonitor.web.monitor.widget;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.filter.StagemonitorSecurityFilter;

public class RequestTraceServletTest {

	private RequestTraceServlet requestTraceServlet;
	private HttpRequestTrace httpRequestTrace;
	private String connectionId;
	private WebPlugin webPlugin;

	@Before
	public void setUp() throws Exception {
		webPlugin = mock(WebPlugin.class);
		Mockito.when(webPlugin.isWidgetEnabled()).thenReturn(Boolean.TRUE);
		requestTraceServlet = new RequestTraceServlet(webPlugin);
		connectionId = UUID.randomUUID().toString();
		httpRequestTrace = new HttpRequestTrace(null, new RequestTrace.GetNameCallback() {
			@Override
			public String getName() {
				return "test";
			}
		}, "/test", Collections.<String, String>emptyMap(), "GET", null, connectionId);
	}

	@Test
	public void testRequestTraceBeforeRequest() throws Exception {
		requestTraceServlet.reportRequestTrace(httpRequestTrace);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", connectionId);
		MockHttpServletResponse response = new MockHttpServletResponse();

		requestTraceServlet.service(request, response);

		Assert.assertEquals(JsonUtils.toJson(Arrays.asList(httpRequestTrace)), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testTwoRequestTraceBeforeRequest() throws Exception {
		requestTraceServlet.reportRequestTrace(httpRequestTrace);
		requestTraceServlet.reportRequestTrace(httpRequestTrace);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", connectionId);
		MockHttpServletResponse response = new MockHttpServletResponse();

		requestTraceServlet.service(request, response);

		Assert.assertEquals(Arrays.asList(httpRequestTrace.toJson(), httpRequestTrace.toJson()).toString(), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testRequestTraceAfterRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", connectionId);
		request.setAsyncSupported(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		requestTraceServlet.service(request, response);

		requestTraceServlet.reportRequestTrace(httpRequestTrace);

		Assert.assertEquals(JsonUtils.toJson(Arrays.asList(httpRequestTrace)), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testRequestTraceAfterRequestAsyncNotSupported() throws Exception {
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", connectionId);
		request.setAsyncSupported(false);
		final MockHttpServletResponse response = new MockHttpServletResponse();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					requestTraceServlet.service(request, response);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();

		Thread.sleep(500);
		requestTraceServlet.reportRequestTrace(httpRequestTrace);
		Thread.sleep(500);

		Assert.assertEquals(JsonUtils.toJson(Arrays.asList(httpRequestTrace)), response.getContentAsString());
		Assert.assertEquals("application/json;charset=UTF-8", response.getHeader("content-type"));
	}

	@Test
	public void testRequestTraceAfterRequestDifferentConnection() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", UUID.randomUUID().toString());
		request.setAsyncSupported(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		requestTraceServlet.service(request, response);

		requestTraceServlet.reportRequestTrace(httpRequestTrace);

		Assert.assertEquals("", response.getContentAsString());
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
		Mockito.when(webPlugin.isWidgetEnabled()).thenReturn(Boolean.FALSE);
		Mockito.when(webPlugin.isWidgetAndStagemonitorEndpointsAllowed(any(MockHttpServletRequest.class), any(Configuration.class))).thenReturn(Boolean.FALSE);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", "");
		MockHttpServletResponse response = new MockHttpServletResponse();

		Configuration configuration = mock(Configuration.class);
		when(configuration.getConfig(WebPlugin.class)).thenReturn(webPlugin);
		new MockFilterChain(requestTraceServlet, new StagemonitorSecurityFilter(configuration)).doFilter(request, response);

		Assert.assertEquals(404, response.getStatus());
		Assert.assertFalse(requestTraceServlet.isActive());
	}
}
