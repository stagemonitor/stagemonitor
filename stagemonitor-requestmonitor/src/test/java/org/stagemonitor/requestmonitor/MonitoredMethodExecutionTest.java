package org.stagemonitor.requestmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class MonitoredMethodExecutionTest {

	private RequestMonitor.RequestInformation<RequestTrace> requestInformation1;
	private RequestMonitor.RequestInformation<RequestTrace> requestInformation2;
	private RequestMonitor.RequestInformation<RequestTrace> requestInformation3;
	private CorePlugin corePlugin = mock(CorePlugin.class);
	private RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
	private final Metric2Registry registry = new Metric2Registry();
	private TestObject testObject = new TestObject(new RequestMonitor(corePlugin, registry, requestMonitorPlugin));

	@Before
	public void clearState() {
		when(requestMonitorPlugin.getNoOfWarmupRequests()).thenReturn(0);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);
		requestInformation1 = requestInformation2 = requestInformation3 = null;
	}

	@Test
	public void testDoubleForwarding() throws Exception {
		testObject.monitored1();
		assertEquals(1, requestInformation1.getExecutionResult());
		assertFalse(requestInformation1.isForwarded());
		assertEquals("monitored1()", requestInformation1.requestTrace.getName());
		assertEquals("1, test", requestInformation1.requestTrace.getParameter());
		assertTrue(requestInformation2.isForwarded());
		assertTrue(requestInformation3.isForwarded());

		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "monitored1()").tag("tier", "server").tag("layer", "total").build()));
		assertNull(registry.getTimers().get(name("response_time").tag("request_name", "monitored2()").tag("tier", "server").tag("layer", "total").build()));
		assertNull(registry.getTimers().get(name("response_time").tag("request_name", "monitored3()").tag("tier", "server").tag("layer", "total").build()));
		assertNull(registry.getTimers().get(name("response_time").tag("request_name", "notMonitored()").tag("tier", "server").tag("layer", "total").build()));
	}

	@Test
	public void testNormalForwarding() throws Exception {
		testObject.monitored3();
		assertEquals(1, requestInformation3.getExecutionResult());

		assertNull(registry.getTimers().get(name("response_time").tag("request_name", "monitored1()").tag("tier", "server").tag("layer", "total").build()));
		assertNull(registry.getTimers().get(name("response_time").tag("request_name", "monitored2()").tag("tier", "server").tag("layer", "total").build()));
		assertNotNull(registry.getTimers().get(name("response_time").tag("request_name", "monitored3()").tag("tier", "server").tag("layer", "total").build()));
		assertNull(registry.getTimers().get(name("response_time").tag("request_name", "notMonitored()").tag("tier", "server").tag("layer", "total").build()));
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
