package org.stagemonitor.web.monitor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.web.configuration.ConfigurationServlet;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.stagemonitor.web.WebPlugin.STAGEMONITOR_PASSWORD;

public class UpdateConfigurationTest {

	private Configuration configuration;
	private ConfigurationServlet configurationServlet;

	@Before
	public void initFilter() throws ServletException {
		configuration = Mockito.spy(new Configuration());
		configurationServlet = new ConfigurationServlet(configuration);
	}

	@Test
	public void testUpdateConfigurationWithoutPasswordSet() throws IOException, ServletException {
		assertNull(configuration.getString("stagemonitor.testUpdateConfiguration"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitor.testUpdateConfiguration", "test");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertNull(configuration.getString("stagemonitor.testUpdateConfiguration"));
	}

	@Test
	public void testUpdateConfiguration() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("");
		assertNull(configuration.getString("stagemonitor.testUpdateConfiguration"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitor.testUpdateConfiguration", "test");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("test", configuration.getString("stagemonitor.testUpdateConfiguration"));
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("");
		assertEquals("", configuration.getString(STAGEMONITOR_PASSWORD));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter(STAGEMONITOR_PASSWORD, "pwd");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("", configuration.getString(STAGEMONITOR_PASSWORD));
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("pwd");
		assertNull(configuration.getString("stagemonitor.testUpdateConfigurationWithoutPassword"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitor.testUpdateConfigurationWithoutPassword", "test");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertNull(configuration.getString("stagemonitor.testUpdateConfigurationWithoutPassword"));
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("pwd");
		assertNull(configuration.getString("stagemonitor.testUpdateConfigurationWithPassword"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitor.testUpdateConfigurationWithPassword", "test");
		request.addParameter(STAGEMONITOR_PASSWORD, "pwd");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("test", configuration.getString("stagemonitor.testUpdateConfigurationWithPassword"));
	}
}
