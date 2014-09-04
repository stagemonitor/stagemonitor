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
		assertEquals("false", configuration.getString("stagemonitor.internal.monitoring"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("false", configuration.getString("stagemonitor.internal.monitoring"));
	}

	@Test
	public void testUpdateConfiguration() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("");
		assertEquals("false", configuration.getString("stagemonitor.internal.monitoring"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("true", configuration.getString("stagemonitor.internal.monitoring"));
	}

	@Test
	public void testUpdateConfigurationNonDynamic() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("");
		assertEquals("60", configuration.getString("stagemonitor.reporting.interval.console"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.reporting.interval.console");
		request.addParameter("stagemonitorConfigValue", "1");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("60", configuration.getString("stagemonitor.reporting.interval.console"));
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("");
		assertEquals("", configuration.getString(STAGEMONITOR_PASSWORD));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", STAGEMONITOR_PASSWORD);
		request.addParameter("stagemonitorConfigValue", "pwd");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("", configuration.getString(STAGEMONITOR_PASSWORD));
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("pwd");
		assertEquals("false", configuration.getString("stagemonitor.internal.monitoring"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("false", configuration.getString("stagemonitor.internal.monitoring"));
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("pwd");
		assertEquals("false", configuration.getString("stagemonitor.internal.monitoring"));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		request.addParameter(STAGEMONITOR_PASSWORD, "pwd");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals("true", configuration.getString("stagemonitor.internal.monitoring"));
	}
}
