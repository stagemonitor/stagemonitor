package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;

public class RequestMonitorPluginConfigurationTest {

	private RequestMonitorPlugin config;

	@Before
	public void before() throws Exception {
		Stagemonitor.reset();
		Configuration configuration = new Configuration(StagemonitorPlugin.class);
		Method registerPluginConfiguration = Configuration.class.getDeclaredMethod("registerOptionProvider", ConfigurationOptionProvider.class);
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
		assertEquals(false, config.isLogCallStacks());
		assertEquals("1w", config.getRequestTraceTtl());
	}
}
