package org.stagemonitor.web.monitor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.filter.HttpRequestMonitorFilter;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class UpdateConfigurationTest {

	private Configuration configuration;
	private HttpRequestMonitorFilter monitorFilter;

	@Before
	public void initFilter() throws ServletException {
		configuration = Mockito.spy(new Configuration());
		monitorFilter = new HttpRequestMonitorFilter(configuration);
		monitorFilter.init(new MockFilterConfig());
	}

	@Test
	public void testUpdateConfigurationWithoutPasswordSet() throws IOException, ServletException {
		assertNull(configuration.getString("stagemonitor.testUpdateConfiguration"));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/doesNotMatter");
		request.addParameter("stagemonitor.testUpdateConfiguration", "test");
		monitorFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

		assertNull(configuration.getString("stagemonitor.testUpdateConfiguration"));
	}

	@Test
	public void testUpdateConfiguration() throws IOException, ServletException {
		when(configuration.getString(null)).thenReturn("");
		assertNull(configuration.getString("stagemonitor.testUpdateConfiguration"));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/doesNotMatter");
		request.addParameter("stagemonitor.testUpdateConfiguration", "test");
		monitorFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

		assertEquals("test", configuration.getString("stagemonitor.testUpdateConfiguration"));
	}

	@Test
	public void testSetNewPasswordViaQueryParamsShouldFail() throws IOException, ServletException {
		when(configuration.getString(null)).thenReturn("");
		assertEquals("", configuration.getString(WebPlugin.STAGEMONITOR_PASSWORD));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/doesNotMatter");
		request.addParameter(WebPlugin.STAGEMONITOR_PASSWORD, "pwd");
		monitorFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

		assertEquals("", configuration.getString(WebPlugin.STAGEMONITOR_PASSWORD));
	}

	@Test
	public void testUpdateConfigurationWithoutPassword() throws IOException, ServletException {
		when(configuration.getString(null)).thenReturn("pwd");
		assertNull(configuration.getString("stagemonitor.testUpdateConfigurationWithoutPassword"));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/doesNotMatter");
		request.addParameter("stagemonitor.testUpdateConfigurationWithoutPassword", "test");
		monitorFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

		assertNull(configuration.getString("stagemonitor.testUpdateConfigurationWithoutPassword"));
	}

	@Test
	public void testUpdateConfigurationWithPassword() throws IOException, ServletException {
		when(configuration.getString(null)).thenReturn("pwd");
		assertNull(configuration.getString("stagemonitor.testUpdateConfigurationWithPassword"));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/doesNotMatter");
		request.addParameter("stagemonitor.testUpdateConfigurationWithPassword", "test");
		request.addParameter(WebPlugin.STAGEMONITOR_PASSWORD, "pwd");
		monitorFilter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

		assertEquals("test", configuration.getString("stagemonitor.testUpdateConfigurationWithPassword"));
	}
}
