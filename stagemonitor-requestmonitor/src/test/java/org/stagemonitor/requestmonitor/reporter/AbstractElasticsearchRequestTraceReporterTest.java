package org.stagemonitor.requestmonitor.reporter;

import com.uber.jaeger.Span;
import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Before;
import org.slf4j.Logger;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.requestmonitor.reporter.ServerRequestMetricsSpanInterceptor.getTimerMetricName;

public class AbstractElasticsearchRequestTraceReporterTest {
	protected ElasticsearchClient elasticsearchClient;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected Logger requestTraceLogger;
	protected Metric2Registry registry;
	protected Configuration configuration;
	protected CorePlugin corePlugin;

	@Before
	public void setUp() throws Exception {
		configuration = mock(Configuration.class);
		corePlugin = mock(CorePlugin.class);
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.singleton("Report Me"));
		when(requestMonitorPlugin.getTracer()).thenReturn(new com.uber.jaeger.Tracer.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true)).build());
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient = mock(ElasticsearchClient.class));
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(true);
		registry = new Metric2Registry();
		when(corePlugin.getMetricRegistry()).thenReturn(registry);
		requestTraceLogger = mock(Logger.class);
	}

	protected Span createTestSpanWithCallTree(long executionTimeMs) {
		final io.opentracing.Span span = createTestSpan(executionTimeMs);
		SpanUtils.setCallTree(span, CallStackElement.createRoot("test"));
		registry.timer(getTimerMetricName("Report Me")).update(executionTimeMs, TimeUnit.MILLISECONDS);
		return (Span) span;
	}

	protected io.opentracing.Span createTestSpan(long executionTimeMs) {
		final Tracer tracer = requestMonitorPlugin.getTracer();
		final io.opentracing.Span span;
		span = tracer.buildSpan("Report Me").withStartTimestamp(1).start();
		Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_SERVER);
		span.finish(TimeUnit.MILLISECONDS.toMicros(executionTimeMs) + 1);
		return span;
	}
}
