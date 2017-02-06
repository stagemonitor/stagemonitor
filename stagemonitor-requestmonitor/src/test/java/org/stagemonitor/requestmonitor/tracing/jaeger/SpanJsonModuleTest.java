package org.stagemonitor.requestmonitor.tracing.jaeger;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uber.jaeger.Tracer;
import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.util.JsonUtils;

import java.util.concurrent.TimeUnit;

import io.opentracing.Span;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SpanJsonModuleTest {

	@Before
	public void setUp() throws Exception {
		JsonUtils.getMapper().registerModule(new SpanJsonModule());
	}

	@Test
	public void testNestDottedTagKeys() {
		final Span span = createTestSpan(1);
		span.setTag("a.b.c.d1", "1");
		span.setTag("a.b.c.d2", "2");
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		System.out.println(jsonSpan);
		assertEquals("1", jsonSpan.get("a").get("b").get("c").get("d1").asText());
		assertEquals("2", jsonSpan.get("a").get("b").get("c").get("d2").asText());
	}

	@Test
	public void testSampledTag() {
		final Span span = createTestSpan(1);
		span.setTag("duration", "foo");
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		System.out.println(jsonSpan);
		assertEquals(1000, jsonSpan.get("duration").intValue());
	}

	@Test
	public void testAmbiguousMapping() {
		final Span span = createTestSpan(1);
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

	private Span createTestSpan(int durationMs) {
		final Span span = new Tracer
				.Builder(getClass().getSimpleName(), new NoopReporter(), new ConstSampler(true))
				.build()
				.buildSpan("test")
				.withStartTimestamp(1)
				.start();
		span.finish(TimeUnit.MILLISECONDS.toMicros(durationMs) + 1);
		return span;
	}

}
