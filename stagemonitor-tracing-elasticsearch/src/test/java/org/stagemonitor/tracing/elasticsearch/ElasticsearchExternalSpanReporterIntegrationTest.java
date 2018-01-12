package org.stagemonitor.tracing.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.AbstractElasticsearchTest;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.IOUtils;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ElasticsearchExternalSpanReporterIntegrationTest extends AbstractElasticsearchTest {

	protected ElasticsearchSpanReporter reporter;
	protected TracingPlugin tracingPlugin;
	protected ConfigurationRegistry configuration;
	protected final MockTracer mockTracer = new MockTracer(new ThreadLocalScopeManager(), new B3Propagator());

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
		reporter = new ElasticsearchSpanReporter();
		reporter.init(configuration);
		final String mappingTemplate = IOUtils.getResourceAsString("stagemonitor-elasticsearch-span-index-template.json");
		elasticsearchClient.sendMappingTemplate(mappingTemplate, "stagemonitor-spans");
		elasticsearchClient.waitForCompletion();
		when(tracingPlugin.getTracer()).thenReturn(mockTracer);
	}

	@Test
	public void reportTemplateCreated() throws Exception {
		final JsonNode template = elasticsearchClient.getJson("/_template/stagemonitor-spans").get("stagemonitor-spans");
		assertThat(template.get("index_patterns").get(0).asText()).isEqualTo("stagemonitor-spans-*");
		assertThat(template.get("mappings").get("_default_").get("_all").get("enabled").asBoolean()).as(template.toString()).isEqualTo(false);
	}

	@Test
	public void reportSpan() throws Exception {
		reporter.report(mock(SpanContextInformation.class), getSpan(100));
		elasticsearchClient.waitForCompletion();
		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-spans*/_search").get("hits");
		assertThat(hits.get("total").intValue()).as(hits.toString()).isEqualTo(1);
		final JsonNode spanJson = hits.get("hits").elements().next().get("_source");
		assertThat(spanJson.get("type").asText()).as(spanJson.toString()).isEqualTo("jdbc");
		assertThat(spanJson.get("method").asText()).as(spanJson.toString()).isEqualTo("SELECT");
		assertThat(spanJson.get("db.statement")).as(spanJson.toString()).isNotNull();
		assertThat(spanJson.get("db.statement").asText()).as(spanJson.toString()).isEqualTo("SELECT * from STAGEMONITOR where 1 < 2");
		assertThat(spanJson.get("duration_ms").asInt()).as(spanJson.toString()).isEqualTo(100);
		assertThat(spanJson.get("name").asText()).as(spanJson.toString()).isEqualTo("ElasticsearchExternalSpanReporterIntegrationTest#test");
	}

	private SpanWrapper getSpan(long executionTimeMillis) {
		final SpanWrapper span = new SpanWrapper(mockTracer.buildSpan("test").start(), "test", 0, 0, Collections.emptyList(), new ConcurrentHashMap<>());
		span.setOperationName("ElasticsearchExternalSpanReporterIntegrationTest#test");
		span.setTag("type", "jdbc");
		span.setTag("method", "SELECT");
		span.setTag("db.statement", "SELECT * from STAGEMONITOR where 1 < 2");
		span.setTag(Tags.PEER_SERVICE.getKey(), "foo@jdbc:bar");
		span.finish(TimeUnit.MILLISECONDS.toMicros(executionTimeMillis));
		return span;
	}
}
