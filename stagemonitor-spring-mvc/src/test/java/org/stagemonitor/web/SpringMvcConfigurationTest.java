package org.stagemonitor.web;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.springmvc.SpringMvcPlugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


public class SpringMvcConfigurationTest {

	private Configuration configuration = new Configuration();

	@Before
	public void before() {
		final SpringMvcPlugin springMvcPlugin = new SpringMvcPlugin();
		springMvcPlugin.initializePlugin(mock(MetricRegistry.class), configuration);
	}

	@Test
	public void testDefaultValues() {
		assertEquals(false, configuration.getBoolean(SpringMvcPlugin.MONITOR_ONLY_SPRING_MVC_REQUESTS));
	}
}
