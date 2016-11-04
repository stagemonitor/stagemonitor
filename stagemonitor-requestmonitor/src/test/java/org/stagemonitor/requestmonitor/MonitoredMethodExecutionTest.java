package org.stagemonitor.requestmonitor;

import com.uber.jaeger.Tracer;
import com.uber.jaeger.reporters.LoggingReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.MetricsReporterTestHelper;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.utils.SpanTags;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MonitoredMethodExecutionTest {

	private RequestMonitor.RequestInformation requestInformation1;
	private RequestMonitor.RequestInformation requestInformation2;
	private RequestMonitor.RequestInformation requestInformation3;
	private final Metric2Registry registry = new Metric2Registry();
	private TestObject testObject;
	private Configuration configuration;

	@Before
	public void clearState() {
		CorePlugin corePlugin = mock(CorePlugin.class);
		RequestMonitorPlugin requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		configuration = mock(Configuration.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);

		when(requestMonitorPlugin.getNoOfWarmupRequests()).thenReturn(0);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(corePlugin.getMetricRegistry()).thenReturn(registry);
		when(requestMonitorPlugin.isCollectRequestStats()).thenReturn(true);

		final Tracer tracer = new Tracer.Builder("RequestMonitorTest", new LoggingReporter(), new ConstSampler(true)).build();
		when(requestMonitorPlugin.getTracer()).thenReturn(tracer);

		requestInformation1 = requestInformation2 = requestInformation3 = null;
		testObject = new TestObject(new RequestMonitor(configuration, registry));
	}

	@Test
	public void testDoubleForwarding() throws Exception {
		testObject.monitored1();
		assertEquals(1, requestInformation1.getExecutionResult());
		assertFalse(requestInformation1.isForwarded());
		assertEquals("monitored1()", requestInformation1.getInternalSpan().getOperationName());
		final Map<String, Object> tags = requestInformation1.getInternalSpan().getTags();
		assertEquals(tags.toString(), "1", tags.get(SpanTags.PARAMETERS_PREFIX + "arg0"));
		assertEquals(tags.toString(), "test", tags.get(SpanTags.PARAMETERS_PREFIX + "arg1"));
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
					new MonitoredMethodRequest(configuration, "monitored1()", new MonitoredMethodRequest.MethodExecution() {
						@Override
						public Object execute() throws Exception {
							return monitored2();
						}
					}, MetricsReporterTestHelper.<String, Object>map("arg0", 1).add("arg1", "test")));
			return (Integer) requestInformation1.getExecutionResult();
		}

		private int monitored2() throws Exception {
			requestInformation2 = requestMonitor.monitor(
					new MonitoredMethodRequest(configuration, "monitored2()", new MonitoredMethodRequest.MethodExecution() {
						@Override
						public Object execute() throws Exception {
							return monitored3();
						}
					}));
			return (Integer) requestInformation2.getExecutionResult();
		}

		private int monitored3() throws Exception {
			requestInformation3 = requestMonitor.monitor(
					new MonitoredMethodRequest(configuration, "monitored3()", new MonitoredMethodRequest.MethodExecution() {
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
