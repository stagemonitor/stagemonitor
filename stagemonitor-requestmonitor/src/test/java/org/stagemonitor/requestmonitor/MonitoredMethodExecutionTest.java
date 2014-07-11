package org.stagemonitor.requestmonitor;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.junit.Test;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.StageMonitor.getMetricRegistry;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

public class MonitoredMethodExecutionTest {

	private RequestMonitor.RequestInformation<RequestTrace> requestInformation1;
	private RequestMonitor.RequestInformation<RequestTrace> requestInformation2;
	private RequestMonitor.RequestInformation<RequestTrace> requestInformation3;

	public void clearState() {
		getMetricRegistry().removeMatching(new MetricFilter() {
			@Override
			public boolean matches(String name, Metric metric) {
				return true;
			}
		});
		requestInformation1 = requestInformation2 = requestInformation3 = null;
	}

	@Test
	public void testDoubleForwarding() throws Exception {
		clearState();
		TestObject testObject = new TestObject(new RequestMonitor());
		testObject.monitored1();
		assertEquals(1, requestInformation1.getExecutionResult());
		assertFalse(requestInformation1.forwardedExecution);
		assertEquals("monitored1()", requestInformation1.requestTrace.getName());
		assertEquals("1, test", requestInformation1.requestTrace.getParameter());
		assertTrue(requestInformation2.forwardedExecution);
		assertNull("monitored2()", requestInformation2.requestTrace); // forwarded method executions are not monitored
		assertTrue(requestInformation3.forwardedExecution);
		assertNull("monitored3()", requestInformation3.requestTrace); // forwarded method executions are not monitored

		assertNotNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("monitored1()"), "time", "server")));
		assertNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("monitored2()"), "time", "server")));
		assertNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("monitored3()"), "time", "server")));
		assertNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("notMonitored()"), "time", "server")));
	}

	@Test
	public void testNormalForwarding() throws Exception {
		clearState();
		TestObject testObject = new TestObject(new RequestMonitor());
		testObject.monitored3();
		assertEquals(1, requestInformation3.getExecutionResult());

		assertNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("monitored1()"), "time", "server")));
		assertNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("monitored2()"), "time", "server")));
		assertNotNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("monitored3()"), "time", "server")));
		assertNull(getMetricRegistry().getTimers().get(name("request", sanitizeGraphiteMetricSegment("notMonitored()"), "time", "server")));
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
