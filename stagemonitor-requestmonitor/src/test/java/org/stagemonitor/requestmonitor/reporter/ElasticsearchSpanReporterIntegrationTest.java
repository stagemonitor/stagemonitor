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
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchSpanReporterIntegrationTest extends AbstractElasticsearchTest {

	protected ElasticsearchSpanReporter reporter;
	protected RequestMonitorPlugin requestMonitorPlugin;
	protected Configuration configuration;

	@Before
	public void setUp() throws Exception {
		this.configuration = mock(Configuration.class);
		this.requestMonitorPlugin = mock(RequestMonitorPlugin.class);
		when(configuration.getConfig(CorePlugin.class)).thenReturn(corePlugin);
		when(configuration.getConfig(RequestMonitorPlugin.class)).thenReturn(requestMonitorPlugin);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(requestMonitorPlugin.getOnlyReportNSpansPerMinute()).thenReturn(1000000d);
		when(requestMonitorPlugin.isPseudonymizeUserNames()).thenReturn(true);
		reporter = new ElasticsearchSpanReporter();
		reporter.init(new SpanReporter.InitArguments(configuration, mock(Metric2Registry.class)));
		when(requestMonitorPlugin.getTracer()).thenReturn(new Tracer.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true)).build());
	}

	@Test
	public void reportSpan() throws Exception {
		final Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("attr.Color", "Blue");
		parameters.put("attr", "bla");
		parameters.put("foo", "bar");
		final MonitoredMethodRequest monitoredMethodRequest = new MonitoredMethodRequest(configuration, "Test#test", null, parameters);
		final Span span = monitoredMethodRequest.createSpan();
		span.setTag("foo.bar", "baz");
		span.finish();
		reporter.report(RequestMonitor.RequestInformation.of(span, null, Collections.<String, Object>emptyMap()));
		elasticsearchClient.waitForCompletion();
		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-spans*/_search").get("hits");
		assertEquals(1, hits.get("total").intValue());
		final JsonNode spanJson = hits.get("hits").elements().next().get("_source");
		assertEquals("baz", spanJson.get("foo").get("bar").asText());
		assertEquals("bar", spanJson.get("parameters").get("foo").asText());
		assertEquals("Blue", spanJson.get("parameters").get("attr_(dot)_Color").asText());
	}

}
