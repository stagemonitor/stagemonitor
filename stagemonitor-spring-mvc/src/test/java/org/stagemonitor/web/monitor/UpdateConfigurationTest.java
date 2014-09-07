package org.stagemonitor.web.monitor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.web.configuration.ConfigurationServlet;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.stagemonitor.web.WebPlugin.STAGEMONITOR_PASSWORD;

public class UpdateConfigurationTest {

	private Configuration configuration;
	private CorePlugin corePlugin;
	private ConfigurationServlet configurationServlet;

	@Before
	public void initFilter() throws ServletException {
		configuration = Mockito.spy(new Configuration(StageMonitorPlugin.class));
		corePlugin = configuration.getConfig(CorePlugin.class);
		configurationServlet = new ConfigurationServlet(configuration);
	}

	@Test
	public void testUpdateConfigurationWithoutPasswordSet() throws IOException, ServletException {
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfiguration() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("");
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertTrue(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNonDynamic() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("");
		assertEquals(60, corePlugin.getConsoleReportingInterval());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.reporting.interval.console");
		request.addParameter("stagemonitorConfigValue", "1");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertEquals(60, corePlugin.getConsoleReportingInterval());
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
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException, ServletException {
		when(configuration.getString(STAGEMONITOR_PASSWORD)).thenReturn("pwd");
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		request.addParameter(STAGEMONITOR_PASSWORD, "pwd");
		configurationServlet.service(request, new MockHttpServletResponse());

		assertTrue(corePlugin.isInternalMonitoringActive());
	}
}
