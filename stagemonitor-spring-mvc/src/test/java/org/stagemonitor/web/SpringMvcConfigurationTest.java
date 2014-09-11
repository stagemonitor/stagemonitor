package org.stagemonitor.web;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;
import org.stagemonitor.springmvc.SpringMvcPlugin;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;


public class SpringMvcConfigurationTest {

	private Configuration configuration = new Configuration(StageMonitorPlugin.class);

	@Before
	public void before() throws Exception{
		Method registerPluginConfiguration = Configuration.class.getDeclaredMethod("registerPluginConfiguration", ConfigurationOptionProvider.class);
		registerPluginConfiguration.setAccessible(true);
		registerPluginConfiguration.invoke(configuration, new SpringMvcPlugin());
	}

	@Test
	public void testDefaultValues() {
		assertEquals(false, configuration.getConfig(SpringMvcPlugin.class).isMonitorOnlySpringMvcRequests());
	}
}
