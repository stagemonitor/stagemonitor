package org.stagemonitor.requestmonitor;

import com.codahale.metrics.SharedMetricRegistries;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class RequestMonitorPluginConfigurationTest {

	private RequestMonitorPlugin config;

	@Before
	public void before() throws Exception {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
		Configuration configuration = new Configuration(Collections.singletonList(new RequestMonitorPlugin()), Collections.emptyList(), "");
		config = configuration.getConfig(RequestMonitorPlugin.class);
	}

	@After
	public void cleanUp() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Test
	public void testDefaultValues() {
		assertEquals(false, config.isCollectCpuTime());

		assertEquals(1000000d, config.getProfilerRateLimitPerMinute(), 0);
		assertEquals(false, config.isLogSpans());
	}
}
