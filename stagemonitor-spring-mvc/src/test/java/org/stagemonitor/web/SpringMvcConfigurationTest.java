package org.stagemonitor.web;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;
import org.stagemonitor.springmvc.SpringMvcPlugin;

import static org.junit.Assert.assertEquals;


public class SpringMvcConfigurationTest {

	private Configuration configuration = new Configuration(StagemonitorPlugin.class);

	@Before
	public void before() throws Exception{
		Method registerPluginConfiguration = Configuration.class.getDeclaredMethod("registerOptionProvider", ConfigurationOptionProvider.class);
		registerPluginConfiguration.setAccessible(true);
		registerPluginConfiguration.invoke(configuration, new SpringMvcPlugin());
	}

	@Test
	public void testDefaultValues() {
		assertEquals(false, configuration.getConfig(SpringMvcPlugin.class).isMonitorOnlySpringMvcRequests());
	}
}
