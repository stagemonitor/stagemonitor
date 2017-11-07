package org.stagemonitor.tracing.elasticsearch;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TagRecordingSpanEventListener;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.impl.DefaultTracerFactory;
import org.stagemonitor.tracing.metrics.MetricsSpanEventListener;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.profiler.formatter.ShortSignatureFormatter;
import org.stagemonitor.tracing.reporter.ReportingSpanEventListener;
import org.stagemonitor.tracing.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class AbstractElasticsearchSpanReporterTest {
	protected ElasticsearchClient elasticsearchClient;
	protected HttpClient httpClient = mock(HttpClient.class);
	protected TracingPlugin tracingPlugin;
	protected ElasticsearchTracingPlugin elasticsearchTracingPlugin;
	protected Logger spanLogger;
	protected Metric2Registry registry;
	protected ConfigurationRegistry configuration;
	protected CorePlugin corePlugin;
	protected Map<String, Object> tags;
	protected ReportingSpanEventListener reportingSpanEventListener;
	protected MockTracer mockTracer;

	@Before
	public void setUp() throws Exception {
		configuration = mock(ConfigurationRegistry.class);
		corePlugin = mock(CorePlugin.class);
		tracingPlugin = mock(TracingPlugin.class);
		elasticsearchTracingPlugin = spy(new ElasticsearchTracingPlugin());

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		when(configuration.getConfig(ElasticsearchTracingPlugin.class)).thenReturn(elasticsearchTracingPlugin);
		when(tracingPlugin.getDefaultRateLimitSpansPerMinute()).thenReturn(1000000d);
		when(tracingPlugin.getDefaultRateLimitSpansPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getDefaultRateLimitSpansPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getDefaultRateLimitSpansPercentOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getRateLimitSpansPerMinutePercentPerTypeOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.getDefaultRateLimitSpansPercent()).thenReturn(1.0);
		when(tracingPlugin.getRateLimitSpansPerMinutePercentPerType()).thenReturn(Collections.emptyMap());
		when(tracingPlugin.getOnlyReportSpansWithName()).thenReturn(Collections.singleton("Report Me"));
		when(tracingPlugin.isProfilerActive()).thenReturn(true);
		when(tracingPlugin.getProfilerRateLimitPerMinute()).thenReturn(1_000_000d);
		when(tracingPlugin.getCallTreeAsciiFormatter()).thenReturn(new ShortSignatureFormatter());
		when(tracingPlugin.isSampled(any())).then(invocation -> new DefaultTracerFactory().isSampled(invocation.getArgument(0)));
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient = mock(ElasticsearchClient.class));
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(true);
		when(elasticsearchClient.getHttpClient()).thenReturn(httpClient);
		registry = new Metric2Registry();
		when(corePlugin.getMetricRegistry()).thenReturn(registry);
		spanLogger = mock(Logger.class);
		tags = new HashMap<>();
		when(tracingPlugin.getRequestMonitor()).thenReturn(mock(RequestMonitor.class));
		reportingSpanEventListener = new ReportingSpanEventListener(configuration);
		mockTracer = new MockTracer();
		final SpanWrappingTracer tracer = TracingPlugin.createSpanWrappingTracer(mockTracer,
				configuration, registry, TagRecordingSpanEventListener.asList(tags),
				new SamplePriorityDeterminingSpanEventListener(configuration), reportingSpanEventListener);
		when(tracingPlugin.getTracer()).thenReturn(tracer);
		assertThat(tracingPlugin.getTracer().scopeManager().active()).isNull();
	}

	@After
	public void tearDown() throws Exception {
		assertThat(tracingPlugin.getTracer().scopeManager().active()).isNull();
	}

	protected SpanContextInformation reportSpanWithCallTree(long executionTimeMs, String operationName) {
		return reportSpan(executionTimeMs, CallStackElement.createRoot("test"), operationName);
	}

	protected SpanContextInformation reportSpan() {
		return reportSpan(1);
	}

	protected SpanContextInformation reportSpan(long executionTimeMs) {
		return reportSpan(executionTimeMs, "Report Me");
	}

	protected SpanContextInformation reportSpan(long executionTimeMs, String operationName) {
		return reportSpan(executionTimeMs, null, operationName);
	}

	private SpanContextInformation reportSpan(long executionTimeMs, CallStackElement callTree, String operationName) {
		final Tracer tracer = tracingPlugin.getTracer();
		final Span span;
		Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName)
				.withStartTimestamp(1);
		spanBuilder = setStartTags(spanBuilder);

		span = spanBuilder
				.start();
		final SpanContextInformation spanContextInformation = SpanContextInformation.forSpan(span);
		spanContextInformation.setCallTree(callTree);
		// implicitly reports
		span.finish(TimeUnit.MILLISECONDS.toMicros(executionTimeMs) + 1);
		return spanContextInformation;
	}

	protected Tracer.SpanBuilder setStartTags(Tracer.SpanBuilder spanBuilder) {
		return spanBuilder
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
				.withTag(SpanUtils.OPERATION_TYPE, "http")
				.withTag(MetricsSpanEventListener.ENABLE_TRACKING_METRICS_TAG, true);
	}
}
