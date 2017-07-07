package org.stagemonitor.tracing.reporter;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.tracing.tracing.B3Propagator;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.opentracing.Span;
import io.opentracing.mock.MockTracer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SpanJsonModuleTest {

	private MockTracer mockTracer;

	@Before
	public void setUp() throws Exception {
		mockTracer = new MockTracer(new B3Propagator());
	}

	@Test
	public void testNestDottedTagKeys() {
		final SpanWrapper span = createTestSpan(1, s -> {
			s.setTag("a.b.c.d1", "1");
			s.setTag("a.b.c.d2", "2");
		});
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		System.out.println(jsonSpan);
		assertEquals("1", jsonSpan.get("a").get("b").get("c").get("d1").asText());
		assertEquals("2", jsonSpan.get("a").get("b").get("c").get("d2").asText());
	}

	@Test
	public void testSetReservedTagName() {
		final SpanWrapper span = createTestSpan(1, s -> s.setTag("duration_ms", "foo"));
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		assertEquals(jsonSpan.toString(), 1, jsonSpan.get("duration_ms").intValue());
	}

	@Test
	public void testAmbiguousMapping() {
		final SpanWrapper span = createTestSpan(1, s -> {
			s.setTag("a", "1");
			s.setTag("a.b", "2");
		});
		try {
			System.out.println(JsonUtils.toObjectNode(span));
			fail();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			assertTrue(e.getMessage().startsWith("Ambiguous mapping for"));
		}
	}

	private SpanWrapper createTestSpan(int durationMs, Consumer<Span> spanConsumer) {
		final SpanWrapper span = new SpanWrapper(mockTracer.buildSpan("test").start(), "test", 0, 0, Collections.emptyList(), new ConcurrentHashMap<>());
		spanConsumer.accept(span);
		span.finish(TimeUnit.MILLISECONDS.toMicros(durationMs));
		return span;
	}

}
