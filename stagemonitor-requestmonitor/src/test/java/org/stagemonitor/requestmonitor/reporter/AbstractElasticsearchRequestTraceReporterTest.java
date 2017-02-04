package org.stagemonitor.requestmonitor.reporter;

import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Before;
import org.slf4j.Logger;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.TagRecordingSpanInterceptor;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.requestmonitor.metrics.ServerRequestMetricsSpanInterceptor.getTimerMetricName;

public class AbstractElasticsearchRequestTraceReporterTest {
	protected ElasticsearchClient elasticsearchClient;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected Logger requestTraceLogger;
	protected Metric2Registry registry;
	protected Configuration configuration;
	protected CorePlugin corePlugin;
	protected Map<String, Object> tags;
	protected RequestMonitor.RequestInformation requestInformation = mock(RequestMonitor.RequestInformation.class);
	private RequestMonitor requestMonitor;

	@Before
	public void setUp() throws Exception {
		configuration = mock(Configuration.class);
		corePlugin = mock(CorePlugin.class);
		requestMonitorPlugin = mock(RequestMonitorPlugin.class);

		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		when(requestMonitorPlugin.getOnlyReportRequestsWithNameToElasticsearch()).thenReturn(Collections.singleton("Report Me"));
		when(corePlugin.getElasticsearchUrl()).thenReturn("http://localhost:9200");
		when(corePlugin.getElasticsearchUrls()).thenReturn(Collections.singletonList("http://localhost:9200"));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient = mock(ElasticsearchClient.class));
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(true);
		registry = new Metric2Registry();
		when(corePlugin.getMetricRegistry()).thenReturn(registry);
		requestTraceLogger = mock(Logger.class);
		tags = new HashMap<>();
		requestMonitor = mock(RequestMonitor.class);
		when(requestMonitor.getRequestInformation()).thenReturn(requestInformation);
		final com.uber.jaeger.Tracer jaegerTracer = new com.uber.jaeger.Tracer
				.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true)).build();
		when(requestMonitorPlugin.getRequestMonitor()).thenReturn(requestMonitor);
		when(requestMonitorPlugin.getTracer()).thenReturn(RequestMonitorPlugin.getSpanWrappingTracer(jaegerTracer,
				registry, requestMonitorPlugin, requestMonitor, TagRecordingSpanInterceptor.asList(tags)));
	}

	protected RequestMonitor.RequestInformation createTestSpanWithCallTree(long executionTimeMs) {
		final RequestMonitor.RequestInformation info = createTestSpan(executionTimeMs);
		SpanUtils.setCallTree(info.getSpan(), CallStackElement.createRoot("test"));
		info.setCallTree(CallStackElement.createRoot("test"));
		registry.timer(getTimerMetricName("Report Me")).update(executionTimeMs, TimeUnit.MILLISECONDS);
		return info;
	}

	protected RequestMonitor.RequestInformation createTestSpan(long executionTimeMs) {
		final Tracer tracer = requestMonitorPlugin.getTracer();
		final Span span;
		span = tracer.buildSpan("Report Me").withStartTimestamp(1).start();
		RequestMonitor.RequestInformation requestInformation = RequestMonitor.RequestInformation.of(span, "Report Me");
		Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_SERVER);
		when(requestMonitor.getRequestInformation()).thenReturn(requestInformation);
		span.finish(TimeUnit.MILLISECONDS.toMicros(executionTimeMs) + 1);
		return requestInformation;
	}
}
