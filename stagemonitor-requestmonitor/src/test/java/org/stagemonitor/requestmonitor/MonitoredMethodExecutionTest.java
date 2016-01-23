package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class MonitoredMethodExecutionTest {

	private RequestMonitor.RequestInformation<RequestTrace> requestInformation1;
	private RequestMonitor.RequestInformation<RequestTrace> requestInformation2;
	private RequestMonitor.RequestInformation<RequestTrace> requestInformation3;
	private final Metric2Registry registry = new Metric2Registry();
	private TestObject testObject;

	@Before
	public void clearState() {
		CorePlugin corePlugin = mock(CorePlugin.class);
		RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		Configuration configuration = mock(Configuration.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);

		when(requestMonitorPlugin.getNoOfWarmupRequests()).thenReturn(0);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		requestInformation1 = requestInformation2 = requestInformation3 = null;
		testObject = new TestObject(new RequestMonitor(configuration, registry));
	}

	@Test
	public void testDoubleForwarding() throws Exception {
		testObject.monitored1();
		assertEquals(1, requestInformation1.getExecutionResult());
		assertFalse(requestInformation1.isForwarded());
		assertEquals("monitored1()", requestInformation1.requestTrace.getName());
		final Map<String, String> parameters = requestInformation1.requestTrace.getParameters();
		assertEquals("1", parameters.get("0"));
		assertEquals("test", parameters.get("1"));
		assertTrue(requestInformation2.isForwarded());
		assertTrue(requestInformation3.isForwarded());

		assertNotNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored1()").layer("All").build()));
		assertNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored2()").layer("All").build()));
		assertNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored3()").layer("All").build()));
		assertNull(registry.getTimers().get(name("response_time_server").tag("request_name", "notMonitored()").layer("All").build()));
	}

	@Test
	public void testNormalForwarding() throws Exception {
		testObject.monitored3();
		assertEquals(1, requestInformation3.getExecutionResult());

		assertNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored1()").layer("All").build()));
		assertNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored2()").layer("All").build()));
		assertNotNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored3()").layer("All").build()));
		assertNull(registry.getTimers().get(name("response_time_server").tag("request_name", "notMonitored()").layer("All").build()));
	}

	private class TestObject {
		private final RequestMonitor requestMonitor;

		private TestObject(RequestMonitor requestMonitor) {
			this.requestMonitor = requestMonitor;
		}

		private int monitored1() throws Exception {
			requestInformation1 = requestMonitor.monitor(
					new MonitoredMethodRequest("monitored1()", new MonitoredMethodRequest.MethodExecution() {
						@Override
						public Object execute() throws Exception {
							return monitored2();
						}
					}, 1, "test"));
			return (Integer) requestInformation1.getExecutionResult();
		}

		private int monitored2() throws Exception {
			requestInformation2 = requestMonitor.monitor(
					new MonitoredMethodRequest("monitored2()", new MonitoredMethodRequest.MethodExecution() {
						@Override
						public Object execute() throws Exception {
							return monitored3();
						}
					}));
			return (Integer) requestInformation2.getExecutionResult();
		}

		private int monitored3() throws Exception {
			requestInformation3 = requestMonitor.monitor(
					new MonitoredMethodRequest("monitored3()", new MonitoredMethodRequest.MethodExecution() {
						@Override
						public Object execute() throws Exception {
							return notMonitored();
						}
					}));
			System.out.println(requestInformation3);
			return (Integer) requestInformation3.getExecutionResult();
		}

		private int notMonitored() {
			return 1;
		}
	}
}
