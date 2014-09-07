package org.stagemonitor.web.monitor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.SimpleSource;
import org.stagemonitor.web.configuration.ConfigurationServlet;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(401, res.getStatus());
		assertEquals("Update configuration password is not set. Dynamic configuration changes are therefore not allowed.", res.getErrorMessage());
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfiguration() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.of(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertNull(response.getErrorMessage());
		assertEquals(204, response.getStatus());
		assertTrue(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNonDynamic() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.of(STAGEMONITOR_PASSWORD, ""));
		assertEquals(60, corePlugin.getConsoleReportingInterval());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.reporting.interval.console");
		request.addParameter("stagemonitorConfigValue", "1");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(400, res.getStatus());
		assertEquals("Configuration option is not dynamic.", res.getErrorMessage());
		assertEquals(60, corePlugin.getConsoleReportingInterval());
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.of(STAGEMONITOR_PASSWORD, ""));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", STAGEMONITOR_PASSWORD);
		request.addParameter("stagemonitorConfigValue", "pwd");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(400, res.getStatus());
		assertEquals("Config key 'stagemonitor.password' does not exist.", res.getErrorMessage());
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.of(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(401, res.getStatus());
		assertEquals("Wrong password for updating configuration.", res.getErrorMessage());
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.of(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("stagemonitorConfigKey", "stagemonitor.internal.monitoring");
		request.addParameter("stagemonitorConfigValue", "true");
		request.addParameter(STAGEMONITOR_PASSWORD, "pwd");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(204, res.getStatus());
		assertNull(res.getErrorMessage());
		assertTrue(corePlugin.isInternalMonitoringActive());
	}
}
