package org.stagemonitor.tracing.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.stagemonitor.AbstractElasticsearchTest;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.reporter.ReportingSpanEventListener;
import org.stagemonitor.tracing.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.utils.SpanUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ElasticsearchSpanReporterIntegrationTest extends AbstractElasticsearchTest {

	protected ElasticsearchSpanReporter reporter;
	protected TracingPlugin tracingPlugin;
	protected ConfigurationRegistry configuration;
	private Tracer tracer;

	@Before
	public void setUp() throws Exception {
		this.configuration = mock(ConfigurationRegistry.class);
		this.tracingPlugin = mock(TracingPlugin.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(TracingPlugin.class)).thenReturn(tracingPlugin);
		when(configuration.getConfig(ElasticsearchTracingPlugin.class)).thenReturn(spy(new ElasticsearchTracingPlugin()));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(corePlugin.getMetricRegistry()).thenReturn(new Metric2Registry());
		when(tracingPlugin.getDefaultRateLimitSpansPerMinute()).thenReturn(1000000d);
		when(tracingPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(tracingPlugin.isPseudonymizeUserNames()).thenReturn(true);
		when(tracingPlugin.isSampled(any())).thenReturn(true);
		reporter = new ElasticsearchSpanReporter();
		reporter.init(configuration);
		final ReportingSpanEventListener reportingSpanEventListener = new ReportingSpanEventListener(configuration);
		reportingSpanEventListener.addReporter(reporter);
		final SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor = mock(SamplePriorityDeterminingSpanEventListener.class);
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).then(invocation -> invocation.getArgument(1));
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())).then(invocation -> invocation.getArgument(1));
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(ArgumentMatchers.anyString(), ArgumentMatchers.any(Number.class))).then(invocation -> invocation.getArgument(1));
		tracer = TracingPlugin.createSpanWrappingTracer(new MockTracer(new ThreadLocalScopeManager(), new B3Propagator()), configuration,
				new Metric2Registry(), Collections.emptyList(), samplePriorityDeterminingSpanInterceptor, reportingSpanEventListener);
		when(tracingPlugin.getTracer()).thenReturn(tracer);
	}

	@Test
	public void reportSpan() throws Exception {
		final Map<String, String> parameters = new HashMap<>();
		parameters.put("attr.Color", "Blue");
		parameters.put("attr", "bla");
		parameters.put("foo", "bar");
		final Span span = tracer.buildSpan("Test#test")
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
				.start();
		SpanUtils.setParameters(span, parameters);
		span.setTag(SpanUtils.OPERATION_TYPE, "method_invocation");
		span.setTag("foo.bar", "baz");
		span.finish();
		elasticsearchClient.waitForCompletion();

		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-spans*/_search").get("hits");
		assertThat(hits.get("total").intValue()).as(hits.toString()).isEqualTo(1);
		validateSpanJson(hits.get("hits").elements().next().get("_source"));
	}

	@Test
	public void testUpdateSpan() throws Exception {
		final Span span = tracer.buildSpan("Test#test")
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
				.start();
		span.finish();
		elasticsearchClient.waitForCompletion();

		refresh();
		reporter.updateSpan(B3HeaderFormat.getB3Identifiers(tracer, span), null, Collections.singletonMap("foo", "bar"));
		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-spans*/_search").get("hits");
		assertThat(hits.get("total").intValue()).as(hits.toString()).isEqualTo(1);
		final JsonNode spanJson = hits.get("hits").elements().next().get("_source");
		assertThat(spanJson.get("foo").asText()).as(spanJson.toString()).isEqualTo("bar");
	}

	@Test
	public void testUpdateNotYetExistentSpan_eventuallyUpdates() throws Exception {
		final Span span = tracer.buildSpan("Test#test")
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
				.startManual();
		reporter.updateSpan(B3HeaderFormat.getB3Identifiers(tracer, span), null, Collections.singletonMap("foo", "bar"));

		span.finish();
		elasticsearchClient.waitForCompletion();
		refresh();

		reporter.getUpdateReporter().flush();
		refresh();

		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-spans*/_search").get("hits");
		assertThat(hits.get("total").intValue()).as(hits.toString()).isEqualTo(1);
		final JsonNode spanJson = hits.get("hits").elements().next().get("_source");
		assertThat(spanJson.get("foo").asText()).as(spanJson.toString()).isEqualTo("bar");
	}

	private void validateSpanJson(JsonNode spanJson) {
		assertThat(spanJson.get("error").booleanValue()).as(spanJson.toString()).isFalse();
		assertThat(spanJson.get("foo.bar").asText()).as(spanJson.toString()).isEqualTo("baz");
		assertThat(spanJson.get("parameters")).as(spanJson.toString()).isNotNull();
		assertThat(spanJson.get("parameters").size()).as(spanJson.toString()).isEqualTo(3);
		assertThat(spanJson.get("parameters").get(0).get("key")).as(spanJson.toString()).isNotNull();
		assertThat(spanJson.get("parameters").get(0).get("value")).as(spanJson.toString()).isNotNull();
	}

}
