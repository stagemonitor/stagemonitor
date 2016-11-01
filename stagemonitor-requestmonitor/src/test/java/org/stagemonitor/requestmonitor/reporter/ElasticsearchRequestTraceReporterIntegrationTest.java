package org.stagemonitor.requestmonitor.reporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.uber.jaeger.Tracer;
import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.Collections;

import io.opentracing.Span;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchRequestTraceReporterIntegrationTest extends AbstractElasticsearchTest {

	protected ElasticsearchSpanReporter reporter;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected Configuration configuration;
	private Tracer tracer;

	@Before
	public void setUp() throws Exception {
		this.configuration = mock(Configuration.class);
		this.requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(requestMonitorPlugin.getOnlyReportNRequestsPerMinuteToElasticsearch()).thenReturn(1000000d);
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		reporter = new ElasticsearchSpanReporter();
		reporter.init(new SpanReporter.InitArguments(configuration, mock(Metric2Registry.class)));
		tracer = new Tracer.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true)).build();
		when(requestMonitorPlugin.getTracer()).thenReturn(tracer);
	}

	@Test
	public void reportRequestTrace() throws Exception {
		final MonitoredMethodRequest monitoredMethodRequest = new MonitoredMethodRequest(configuration, "Test#test", null, Collections.singletonMap("attr.Color", "Blue"));
		final Span span = monitoredMethodRequest.createSpan();
		span.setTag("foo.bar", "baz");
		span.finish();
		reporter.report(new SpanReporter.ReportArguments(null, span, null));
		elasticsearchClient.waitForCompletion();
		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-spans*/_search").get("hits");
		assertEquals(1, hits.get("total").intValue());
		final JsonNode requestTraceJson = hits.get("hits").elements().next().get("_source");
		assertEquals("baz", requestTraceJson.get("foo_(dot)_bar").asText());
		// TODO revisit that. Ideal would be a nested parameters object
		// use elasticsearch 5.0 feature which converts dots in field names into nested objects
//		assertEquals("Blue", requestTraceJson.get("parameters").get("attr_(dot)_Color").asText());
	}

}
