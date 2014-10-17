package org.stagemonitor.requestmonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class RequestMonitorPluginConfigurationTest {

	private RequestMonitorPlugin config;

	@Before
	public void before() throws Exception {
		Stagemonitor.reset();
		Configuration configuration = new Configuration(StagemonitorPlugin.class);
		Method registerPluginConfiguration = Configuration.class.getDeclaredMethod("registerPluginConfiguration", ConfigurationOptionProvider.class);
		registerPluginConfiguration.setAccessible(true);
		registerPluginConfiguration.invoke(configuration, new RequestMonitorPlugin());
		config = configuration.getConfig(RequestMonitorPlugin.class);
	}

	@After
	public void cleanUp() {
		Stagemonitor.reset();
	}

	@Test
	public void testDefaultValues() {
		assertEquals(0, config.getNoOfWarmupRequests());
		assertEquals(0, config.getNoOfWarmupRequests());
		assertEquals(true, config.isCollectRequestStats());
		assertEquals(false, config.isCollectCpuTime());

		assertEquals(1, config.getCallStackEveryXRequestsToGroup());
		assertEquals(true, config.isLogCallStacks());
		assertEquals("1w", config.getRequestTraceTtl());
	}
}
