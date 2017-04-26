package org.stagemonitor.tracing.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.stagemonitor.AbstractElasticsearchTest;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.reporter.ReadbackSpan;
import org.stagemonitor.requestmonitor.reporter.ReportingSpanEventListener;
import org.stagemonitor.requestmonitor.sampling.SamplePriorityDeterminingSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.B3Propagator;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchSpanReporterIntegrationTest extends AbstractElasticsearchTest {

	protected ElasticsearchSpanReporter reporter;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected ConfigurationRegistry configuration;
	private Tracer tracer;

	@Before
	public void setUp() throws Exception {
		JsonUtils.getMapper().registerModule(new ReadbackSpan.SpanJsonModule());
		this.configuration = mock(ConfigurationRegistry.class);
		this.requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(configuration.getConfig(ElasticsearchTracingPlugin.class)).thenReturn(mock(ElasticsearchTracingPlugin.class));
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(requestMonitorPlugin.getRateLimitServerSpansPerMinute()).thenReturn(1000000d);
		when(requestMonitorPlugin.getProfilerRateLimitPerMinuteOption()).thenReturn(mock(ConfigurationOption.class));
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		reporter = new ElasticsearchSpanReporter();
		reporter.init(configuration);
		final ReportingSpanEventListener reportingSpanEventListener = new ReportingSpanEventListener(configuration);
		reportingSpanEventListener.addReporter(reporter);
		final SamplePriorityDeterminingSpanEventListener samplePriorityDeterminingSpanInterceptor = mock(SamplePriorityDeterminingSpanEventListener.class);
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).then(invocation -> invocation.getArgument(1));
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())).then(invocation -> invocation.getArgument(1));
		when(samplePriorityDeterminingSpanInterceptor.onSetTag(ArgumentMatchers.anyString(), ArgumentMatchers.any(Number.class))).then(invocation -> invocation.getArgument(1));
		tracer = RequestMonitorPlugin.createSpanWrappingTracer(new MockTracer(new B3Propagator()), configuration,
				new Metric2Registry(), Collections.emptyList(), samplePriorityDeterminingSpanInterceptor, reportingSpanEventListener);
		when(requestMonitorPlugin.getTracer()).thenReturn(tracer);
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
		validateSpanJson(JsonUtils.getMapper().valueToTree(SpanContextInformation.forSpan(span).getReadbackSpan()));

		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-spans*/_search").get("hits");
		Assert.assertEquals(1, hits.get("total").intValue());
		validateSpanJson(hits.get("hits").elements().next().get("_source"));
	}

	private void validateSpanJson(JsonNode spanJson) {
		Assert.assertFalse(spanJson.get("error").booleanValue());
		Assert.assertNotNull(spanJson.toString(), spanJson.get("foo"));
		Assert.assertEquals(spanJson.toString(), "baz", spanJson.get("foo").get("bar").asText());
		Assert.assertNotNull(spanJson.toString(), spanJson.get("parameters"));
		Assert.assertEquals(spanJson.toString(), "bar", spanJson.get("parameters").get("foo").asText());
		Assert.assertEquals(spanJson.toString(), "Blue", spanJson.get("parameters").get("attr_(dot)_Color").asText());
	}

}
