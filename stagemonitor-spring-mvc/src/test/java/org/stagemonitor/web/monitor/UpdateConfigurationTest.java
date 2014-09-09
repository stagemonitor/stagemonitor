package org.stagemonitor.web.monitor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationSource;
import org.stagemonitor.core.configuration.SimpleSource;
import org.stagemonitor.core.configuration.SystemPropertyConfigurationSource;
import org.stagemonitor.web.configuration.ConfigurationServlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.StageMonitor.STAGEMONITOR_PASSWORD;

public class UpdateConfigurationTest {

	private Configuration configuration;
	private CorePlugin corePlugin;
	private ConfigurationServlet configurationServlet;

	@Before
	public void initFilter() throws ServletException {
		configuration = Mockito.spy(new Configuration(StageMonitorPlugin.class, STAGEMONITOR_PASSWORD));
		corePlugin = configuration.getConfig(CorePlugin.class);
		configurationServlet = new ConfigurationServlet(configuration);
	}

	@Test
	public void testUpdateConfigurationWithoutPasswordSet() throws IOException, ServletException {
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "Transient Configuration Source");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(401, res.getStatus());
		assertEquals("Update configuration password is not set. Dynamic configuration changes are therefore not allowed.", res.getContentAsString());
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfiguration() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "Transient Configuration Source");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("", response.getContentAsString());
		assertEquals(204, response.getStatus());
		assertTrue(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWithoutConfigurationSource() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("Missing parameter 'configurationSource'", response.getContentAsString());
		assertEquals(400, response.getStatus());
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWrongConfigurationSource() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "foo");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("Configuration source 'foo' does not exist.", response.getContentAsString());
		assertEquals(400, response.getStatus());
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNotSaveableConfigurationSource() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		configuration.addConfigurationSource(new SystemPropertyConfigurationSource());
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "Java System Properties");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		configurationServlet.service(request, response);

		assertEquals("Saving to Java System Properties is not possible.", response.getContentAsString());
		assertEquals(400, response.getStatus());
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationNonDynamicTransient() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		assertEquals(60, corePlugin.getConsoleReportingInterval());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.reporting.interval.console");
		request.addParameter("value", "1");
		request.addParameter("configurationSource", "Transient Configuration Source");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(400, res.getStatus());
		assertEquals("Non dynamic options can't be saved to a transient configuration source.", res.getContentAsString());
		assertEquals(60, corePlugin.getConsoleReportingInterval());
	}

	@Test
	public void testUpdateConfigurationNonDynamicPersistent() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));
		final ConfigurationSource persistentSourceMock = mock(ConfigurationSource.class);
		when(persistentSourceMock.isSavingPossible()).thenReturn(true);
		when(persistentSourceMock.isSavingPersistent()).thenReturn(true);
		when(persistentSourceMock.getName()).thenReturn("Test Persistent");
		configuration.addConfigurationSource(persistentSourceMock);
		assertEquals(60, corePlugin.getConsoleReportingInterval());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.reporting.interval.console");
		request.addParameter("value", "1");
		request.addParameter("configurationSource", "Test Persistent");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals("", res.getContentAsString());
		assertEquals(204, res.getStatus());
		verify(persistentSourceMock).save("stagemonitor.reporting.interval.console", "1");
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, ""));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", STAGEMONITOR_PASSWORD);
		request.addParameter("value", "pwd");
		request.addParameter("configurationSource", "Transient Configuration Source");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(400, res.getStatus());
		assertEquals("Config key 'stagemonitor.password' does not exist.", res.getContentAsString());
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "Transient Configuration Source");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals(401, res.getStatus());
		assertEquals("Wrong password for updating configuration.", res.getContentAsString());
		assertFalse(corePlugin.isInternalMonitoringActive());
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException, ServletException {
		configuration.addConfigurationSource(SimpleSource.forTest(STAGEMONITOR_PASSWORD, "pwd"));
		assertFalse(corePlugin.isInternalMonitoringActive());

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stagemonitor/configuration");
		request.addParameter("key", "stagemonitor.internal.monitoring");
		request.addParameter("value", "true");
		request.addParameter("configurationSource", "Transient Configuration Source");
		request.addParameter(STAGEMONITOR_PASSWORD, "pwd");
		final MockHttpServletResponse res = new MockHttpServletResponse();
		configurationServlet.service(request, res);

		assertEquals("", res.getContentAsString());
		assertEquals(204, res.getStatus());
		assertTrue(corePlugin.isInternalMonitoringActive());
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
}
