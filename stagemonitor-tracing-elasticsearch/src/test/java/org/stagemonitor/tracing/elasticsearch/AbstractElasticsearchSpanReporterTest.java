package org.stagemonitor.tracing.elasticsearch;

import com.uber.jaeger.context.TracingUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.MockTracer;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.TagRecordingSpanEventListener;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.reporter.ReportingSpanEventListener;
import org.stagemonitor.requestmonitor.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.requestmonitor.metrics.ServerRequestMetricsSpanEventListener.getTimerMetricName;

public class AbstractElasticsearchSpanReporterTest {
	protected ElasticsearchClient elasticsearchClient;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected ElasticsearchTracingPlugin elasticsearchTracingPlugin;
	protected Logger spanLogger;
	protected Metric2Registry registry;
	protected ConfigurationRegistry configuration;
	protected CorePlugin corePlugin;
	protected Map<String, Object> tags;
	protected ReportingSpanEventListener reportingSpanEventListener;

	@Before
	public void setUp() throws Exception {
		configuration = mock(ConfigurationRegistry.class);
		corePlugin = mock(CorePlugin.class);
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		elasticsearchTracingPlugin = mock(ElasticsearchTracingPlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(ElasticsearchTracingPlugin.class)).thenReturn(elasticsearchTracingPlugin);
		when(requestMonitorPlugin.getRateLimitServerSpansPerMinute()).thenReturn(1000000d);
		when(requestMonitorPlugin.getRateLimitServerSpansPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(requestMonitorPlugin.getRateLimitClientSpansPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(requestMonitorPlugin.getRateLimitClientSpansPerTypePerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(requestMonitorPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(requestMonitorPlugin.getOnlyReportSpansWithName()).thenReturn(Collections.singleton("Report Me"));
		when(requestMonitorPlugin.isProfilerActive()).thenReturn(true);
		when(requestMonitorPlugin.getProfilerRateLimitPerMinute()).thenReturn(1_000_000d);
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient = mock(ElasticsearchClient.class));
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(true);
		registry = new Metric2Registry();
		when(corePlugin.getMetricRegistry()).thenReturn(registry);
		spanLogger = mock(Logger.class);
		tags = new HashMap<>();
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(mock(RequestMonitor.class));
		reportingSpanEventListener = new ReportingSpanEventListener(configuration);
		final SpanWrappingTracer tracer = RequestMonitorPlugin.createSpanWrappingTracer(new MockTracer(),
				configuration, registry, TagRecordingSpanEventListener.asList(tags),
				new SamplePriorityDeterminingSpanEventListener(configuration), reportingSpanEventListener);
		when(requestMonitorPlugin.getTracer()).thenReturn(tracer);
		Assert.assertTrue(TracingUtils.getTraceContext().isEmpty());
	}

	@After
	public void tearDown() throws Exception {
		Assert.assertTrue(TracingUtils.getTraceContext().isEmpty());
	}

	protected SpanContextInformation reportSpanWithCallTree(long executionTimeMs, String operationName) {
		final SpanContextInformation info = reportSpan(executionTimeMs, CallStackElement.createRoot("test"), operationName);
		registry.timer(getTimerMetricName("Report Me")).update(executionTimeMs, TimeUnit.MILLISECONDS);
		return info;
	}

	private SpanContextInformation reportSpan(long executionTimeMs, CallStackElement callTree, String operationName) {
		final Tracer tracer = requestMonitorPlugin.getTracer();
		final Span span;
		span = tracer.buildSpan(operationName)
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
				.withStartTimestamp(1)
				.start();
		final SpanContextInformation spanContextInformation = SpanContextInformation.forSpan(span);
		spanContextInformation.setCallTree(callTree);
		// implicitly reports
		span.finish(TimeUnit.MILLISECONDS.toMicros(executionTimeMs) + 1);
		return spanContextInformation;
	}
}
