package org.stagemonitor.web.configuration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.configuration.Configuration;

public class ConfigurationServletTest {

	private Configuration configuration = mock(Configuration.class);
	private ConfigurationServlet configurationServlet = new ConfigurationServlet(configuration);

	@Test
	public void testUpdateConfigurationWithoutConfigurationSource() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("Missing parameter 'configurationSource'", response.getContentAsString());
		assertEquals(400, response.getStatus());
	}

	@Test
	public void testUpdateConfigurationWithoutKey() throws IOException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "test");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("Missing parameter 'key'", response.getContentAsString());
		assertEquals(400, response.getStatus());
	}

	@Test
	public void testReload() throws IOException, ServletException {
		for (String method : Arrays.asList("POST", "GET")) {
			MockHttpServletRequest request = new MockHttpServletRequest(method, "/stagemonitor/configuration");
			request.addParameter("reload", "");
			final MockHttpServletResponse res = new MockHttpServletResponse();
			configurationServlet.service(request, res);
			assertEquals(204, res.getStatus());
			assertEquals("", res.getContentAsString());
		}
	}

	@Test
	public void testNoError() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "test");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("", response.getContentAsString());
		assertEquals(204, response.getStatus());
	}

	@Test
	public void testIllegalArgumentException() throws ServletException, IOException {
		doThrow(new IllegalArgumentException("test")).when(configuration).save(any(), any(), any(), any());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "test");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("test", response.getContentAsString());
		assertEquals(400, response.getStatus());
	}

	@Test
	public void testIllegalStateException() throws ServletException, IOException {
		doThrow(new IllegalStateException("test")).when(configuration).save(any(), any(), any(), any());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "test");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("test", response.getContentAsString());
		assertEquals(401, response.getStatus());
	}

	@Test
	public void testUnsupportedOperationException() throws ServletException, IOException {
		doThrow(new UnsupportedOperationException("test")).when(configuration).save(any(), any(), any(), any());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "test");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("test", response.getContentAsString());
		assertEquals(400, response.getStatus());
	}

	@Test
	public void testException() throws ServletException, IOException {
		doThrow(new RuntimeException("test")).when(configuration).save(any(), any(), any(), any());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "test");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("Internal Error. Check your server logs.", response.getContentAsString());
		assertEquals(500, response.getStatus());
	}
}
