package org.stagemonitor.requestmonitor.reporter;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.tracing.jaeger.SpanJsonModule;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SpanJsonModuleTest extends AbstractElasticsearchRequestTraceReporterTest {

	private ElasticsearchSpanReporter reporter;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		reporter = new ElasticsearchSpanReporter(requestTraceLogger);
		reporter.init(new SpanReporter.InitArguments(configuration, registry));
		JsonUtils.getMapper().registerModule(new SpanJsonModule());
	}

	@Test
	public void testNestDottedTagKeys() {
		final io.opentracing.Span span = ((SpanWrapper) createTestSpan(1).getSpan()).getDelegate();
		span.setTag("a.b.c.d1", "1");
		span.setTag("a.b.c.d2", "2");
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		System.out.println(jsonSpan);
		assertEquals("1", jsonSpan.get("a").get("b").get("c").get("d1").asText());
		assertEquals("2", jsonSpan.get("a").get("b").get("c").get("d2").asText());
	}

	@Test
	public void testSampledTag() {
		final io.opentracing.Span span = ((SpanWrapper) createTestSpan(1).getSpan()).getDelegate();
		span.setTag("duration", "foo");
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		System.out.println(jsonSpan);
		assertEquals(1000, jsonSpan.get("duration").intValue());
	}

	@Test
	public void testAmbiguousMapping() {
		final io.opentracing.Span span = ((SpanWrapper) createTestSpan(1).getSpan()).getDelegate();
		span.setTag("a", "1");
		span.setTag("a.b", "2");
		try {
			System.out.println(JsonUtils.toObjectNode(span));
			fail();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			assertTrue(e.getMessage().startsWith("Ambiguous mapping for"));
		}
	}

}
