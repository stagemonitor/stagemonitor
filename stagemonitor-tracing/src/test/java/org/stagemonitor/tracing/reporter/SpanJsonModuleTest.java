package org.stagemonitor.tracing.reporter;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import org.stagemonitor.core.util.JsonUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SpanJsonModuleTest {

	@Test
	public void testNestDottedTagKeys() {
		final ReadbackSpan span = createTestSpan(1);
		span.setTag("a.b.c.d1", "1");
		span.setTag("a.b.c.d2", "2");
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		System.out.println(jsonSpan);
		assertEquals("1", jsonSpan.get("a").get("b").get("c").get("d1").asText());
		assertEquals("2", jsonSpan.get("a").get("b").get("c").get("d2").asText());
	}

	@Test
	public void testSetReservedTagName() {
		final ReadbackSpan span = createTestSpan(1);
		span.setTag("duration_ms", "foo");
		final ObjectNode jsonSpan = JsonUtils.toObjectNode(span);
		assertEquals(jsonSpan.toString(), 1, jsonSpan.get("duration_ms").intValue());
	}

	@Test
	public void testAmbiguousMapping() {
		final ReadbackSpan span = createTestSpan(1);
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

	private ReadbackSpan createTestSpan(int durationMs) {
		final ReadbackSpan readbackSpan = new ReadbackSpan();
		readbackSpan.setDuration(durationMs);
		return readbackSpan;
	}

}
