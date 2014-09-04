package org.stagemonitor.web;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;
import org.stagemonitor.springmvc.SpringMvcPlugin;

import static org.junit.Assert.assertEquals;


public class SpringMvcConfigurationTest {

	private Configuration configuration = new Configuration();

	@Before
	public void before() {
		final SpringMvcPlugin springMvcPlugin = new SpringMvcPlugin();
		for (ConfigurationOption configurationOption : springMvcPlugin.getConfigurationOptions()) {
			configuration.add("Test", configurationOption);
		}
	}

	@Test
	public void testDefaultValues() {
		assertEquals(false, configuration.getBoolean(SpringMvcPlugin.MONITOR_ONLY_SPRING_MVC_REQUESTS));
	}
}
