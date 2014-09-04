package org.stagemonitor.requestmonitor;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;

import static org.junit.Assert.assertEquals;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.CALL_STACK_EVERY_XREQUESTS_TO_GROUP;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.COLLECT_REQUEST_STATS;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.CPU_TIME;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.LOG_CALL_STACKS;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.NO_OF_WARMUP_REQUESTS;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.PROFILER_MIN_EXECUTION_TIME_NANOS;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.REQUEST_TRACE_TTL;
import static org.stagemonitor.requestmonitor.RequestMonitorPlugin.WARMUP_SECONDS;

public class RequestMonitorPluginConfigurationTest {

	private Configuration configuration = new Configuration();

	@Before
	public void before() {
		final RequestMonitorPlugin requestMonitorPlugin = new RequestMonitorPlugin();
		for (ConfigurationOption configurationOption : requestMonitorPlugin.getConfigurationOptions()) {
			configuration.add("Request Monitor Plugin Test", configurationOption);
		}
	}

	@Test
	public void testDefaultValues() {
		assertEquals(0, configuration.getInt(NO_OF_WARMUP_REQUESTS));
		assertEquals(0, configuration.getInt(WARMUP_SECONDS));
		assertEquals(true, configuration.getBoolean(COLLECT_REQUEST_STATS));
		assertEquals(false, configuration.getBoolean(CPU_TIME));

		assertEquals(100000L, configuration.getLong(PROFILER_MIN_EXECUTION_TIME_NANOS));
		assertEquals(1, configuration.getInt(CALL_STACK_EVERY_XREQUESTS_TO_GROUP));
		assertEquals(true, configuration.getBoolean(LOG_CALL_STACKS));
		assertEquals("1w", configuration.getString(REQUEST_TRACE_TTL));
	}
}
