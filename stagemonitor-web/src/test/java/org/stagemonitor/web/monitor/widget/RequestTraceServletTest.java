package org.stagemonitor.web.monitor.widget;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class RequestTraceServletTest {

	private RequestTraceServlet requestTraceServlet;
	private HttpRequestTrace httpRequestTrace;
	private String connectionId;
	private WebPlugin webPlugin;

	@Before
	public void setUp() throws Exception {
		webPlugin = Mockito.mock(WebPlugin.class);
		Mockito.when(webPlugin.isWidgetEnabled()).thenReturn(Boolean.TRUE);
		requestTraceServlet = new RequestTraceServlet(webPlugin);
		connectionId = UUID.randomUUID().toString();
		httpRequestTrace = new HttpRequestTrace(new RequestTrace.GetNameCallback() {
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

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stagemonitor/request-traces");
		request.addParameter("connectionId", "");
		MockHttpServletResponse response = new MockHttpServletResponse();

		requestTraceServlet.service(request, response);

		Assert.assertEquals(404, response.getStatus());
		Assert.assertFalse(requestTraceServlet.isActive());
	}
}
