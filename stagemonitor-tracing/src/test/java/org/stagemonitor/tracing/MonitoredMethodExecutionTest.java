package org.stagemonitor.tracing;

import com.uber.jaeger.context.TracingUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.MetricsReporterTestHelper;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.reporter.ReportingSpanEventListener;
import org.stagemonitor.tracing.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MonitoredMethodExecutionTest {

	private SpanContextInformation spanContext1;
	private SpanContextInformation spanContext2;
	private SpanContextInformation spanContext3;
	private final Metric2Registry registry = new Metric2Registry();
	private TestObject testObject;
	private ConfigurationRegistry configuration;
	private Map<String, Object> tags;

	@Before
	public void clearState() {
		CorePlugin corePlugin = mock(CorePlugin.class);
		TracingPlugin tracingPlugin = mock(TracingPlugin.class);
		when(tracingPlugin.getDefaultRateLimitSpansPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getDefaultRateLimitSpansPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getRateLimitClientSpansPerTypePerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));

		configuration = mock(ConfigurationRegistry.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);

		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(corePlugin.getMetricRegistry()).thenReturn(registry);
		when(corePlugin.getElasticsearchClient()).thenReturn(mock(ElasticsearchClient.class));

		spanContext1 = spanContext2 = spanContext3 = null;
		final RequestMonitor requestMonitor = new RequestMonitor(configuration, registry);
		when(tracingPlugin.getRequestMonitor()).thenReturn(requestMonitor);

		tags = new HashMap<>();
		final SpanWrappingTracer tracer = TracingPlugin.createSpanWrappingTracer(new MockTracer(), configuration, registry,
				TagRecordingSpanEventListener.asList(tags),
				new SamplePriorityDeterminingSpanEventListener(configuration), new ReportingSpanEventListener(configuration));
		when(tracingPlugin.getTracer()).thenReturn(tracer);

		testObject = new TestObject(requestMonitor);
		assertTrue(TracingUtils.getTraceContext().isEmpty());
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TracingUtils.getTraceContext().isEmpty());
	}

	@Test
	public void testDoubleForwarding() throws Exception {
		testObject.monitored1();
		assertEquals("monitored1()", spanContext1.getOperationName());
		assertEquals(tags.toString(), "1", tags.get(SpanUtils.PARAMETERS_PREFIX + "arg0"));
		assertEquals(tags.toString(), "test", tags.get(SpanUtils.PARAMETERS_PREFIX + "arg1"));

		assertNotNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored1()").layer("All").build()));
		assertNotNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored2()").layer("All").build()));
		assertNotNull(registry.getTimers().get(name("response_time_server").tag("request_name", "monitored3()").layer("All").build()));
		assertNull(registry.getTimers().get(name("response_time_server").tag("request_name", "notMonitored()").layer("All").build()));
	}

	@Test
	public void testNormalForwarding() throws Exception {
		testObject.monitored3();

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

		private void monitored1() throws Exception {
			spanContext1 = requestMonitor.monitor(
					new MonitoredMethodRequest(configuration, "monitored1()", this::monitored2, MetricsReporterTestHelper.<String, Object>map("arg0", 1).add("arg1", "test")));
		}

		private void monitored2() throws Exception {
			spanContext2 = requestMonitor.monitor(
					new MonitoredMethodRequest(configuration, "monitored2()", this::monitored3));
		}

		private void monitored3() throws Exception {
			spanContext3 = requestMonitor.monitor(
					new MonitoredMethodRequest(configuration, "monitored3()", this::notMonitored));
		}

		private int notMonitored() {
			return 1;
		}
	}

}
