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
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.MonitoredMethodRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrappingTracer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
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
		final Tracer jaegerTracer = new Tracer.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true)).build();
		when(requestMonitorPlugin.getTracer()).thenReturn(new SpanWrappingTracer(jaegerTracer, Collections.singletonList(new Callable<SpanInterceptor>() {
			@Override
			public SpanInterceptor call() throws Exception {
				return new SpanInterceptor() {
					@Override
					public Number onSetTag(String key, Number value) {
						if (Tags.SAMPLING_PRIORITY.getKey().equals(key) && value.intValue() == 0) {
							fail();
						}
						return value;
					}
				};
			}
		})));
	}

	@Test
	public void reportSpan() throws Exception {
		final Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("attr.Color", "Blue");
		parameters.put("attr", "bla");
		parameters.put("foo", "bar");
		final MonitoredMethodRequest monitoredMethodRequest = new MonitoredMethodRequest(configuration, "Test#test", null, parameters);
		final Span span = monitoredMethodRequest.createSpan(null);
		span.setTag("foo.bar", "baz");
		span.finish();
		reporter.report(RequestMonitor.RequestInformation.of(span, null, Collections.<String, Object>emptyMap()));
		elasticsearchClient.waitForCompletion();
		validateSpanJson(JsonUtils.getMapper().valueToTree(span));

		refresh();
		final JsonNode hits = elasticsearchClient.getJson("/stagemonitor-spans*/_search").get("hits");
		assertEquals(1, hits.get("total").intValue());
		validateSpanJson(hits.get("hits").elements().next().get("_source"));
	}

	private void validateSpanJson(JsonNode spanJson) {
		assertNotNull(spanJson.toString(), spanJson.get("foo"));
		assertEquals(spanJson.toString(), "baz", spanJson.get("foo").get("bar").asText());
		assertNotNull(spanJson.toString(), spanJson.get("parameters"));
		assertEquals(spanJson.toString(), "bar", spanJson.get("parameters").get("foo").asText());
		assertEquals(spanJson.toString(), "Blue", spanJson.get("parameters").get("attr_(dot)_Color").asText());
	}

}
