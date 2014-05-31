package org.stagemonitor.collector.core.monitor;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import org.junit.Test;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.*;
import static org.stagemonitor.collector.core.StageMonitor.getMetricRegistry;
import static org.stagemonitor.collector.core.util.GraphiteEncoder.encodeForGraphite;

public class MonitoredMethodExecutionTest {

	private ExecutionContextMonitor.ExecutionInformation<ExecutionContext> executionInformation1;
	private ExecutionContextMonitor.ExecutionInformation<ExecutionContext> executionInformation2;
	private ExecutionContextMonitor.ExecutionInformation<ExecutionContext> executionInformation3;

	public void clearState() {
		getMetricRegistry().removeMatching(new MetricFilter() {
			@Override
			public boolean matches(String name, Metric metric) {
				return true;
			}
		});
		executionInformation1 = executionInformation2 = executionInformation3 = null;
	}

	@Test
	public void testDoubleForwarding() throws Exception {
		clearState();
		TestObject testObject = new TestObject(new ExecutionContextMonitor());
		testObject.monitored1();
		assertEquals(1, executionInformation1.getExecutionResult());
		assertFalse(executionInformation1.forwardedExecution);
		assertEquals("monitored1()", executionInformation1.executionContext.getName());
		assertTrue(executionInformation2.forwardedExecution);
		assertNull("monitored2()", executionInformation2.executionContext); // forwarded method executions are not monitored
		assertTrue(executionInformation3.forwardedExecution);
		assertNull("monitored3()", executionInformation3.executionContext); // forwarded method executions are not monitored

		assertNotNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("monitored1()"))));
		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("monitored2()"))));
		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("monitored3()"))));
		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("notMonitored()"))));
	}

	@Test
	public void testNormalForwarding() throws Exception {
		clearState();
		TestObject testObject = new TestObject(new ExecutionContextMonitor());
		testObject.monitored3();
		assertEquals(1, executionInformation3.getExecutionResult());

		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("monitored1()"))));
		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("monitored2()"))));
		assertNotNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("monitored3()"))));
		assertNull(getMetricRegistry().getTimers().get(name("request", "total", encodeForGraphite("notMonitored()"))));
	}

	private class TestObject {
		private final ExecutionContextMonitor executionContextMonitor;

		private TestObject(ExecutionContextMonitor executionContextMonitor) {
			this.executionContextMonitor = executionContextMonitor;
		}

		private int monitored1() throws Exception {
			executionInformation1 = executionContextMonitor.monitor(
					new MonitoredMethodExecution("monitored1()", new MonitoredMethodExecution.MethodExecution() {
						@Override
						public Object execute() throws Exception {
							return monitored2();
						}
					}));
			return (Integer) executionInformation1.getExecutionResult();
		}

		private int monitored2() throws Exception {
			executionInformation2 = executionContextMonitor.monitor(
					new MonitoredMethodExecution("monitored2()", new MonitoredMethodExecution.MethodExecution() {
						@Override
						public Object execute() throws Exception {
							return monitored3();
						}
					}));
			return (Integer) executionInformation2.getExecutionResult();
		}

		private int monitored3() throws Exception {
			executionInformation3 = executionContextMonitor.monitor(
					new MonitoredMethodExecution("monitored3()", new MonitoredMethodExecution.MethodExecution() {
						@Override
						public Object execute() throws Exception {
							return notMonitored();
						}
					}));
			System.out.println(executionInformation3);
			return (Integer) executionInformation3.getExecutionResult();
		}

		private int notMonitored() {
			return 1;
		}
	}
}
