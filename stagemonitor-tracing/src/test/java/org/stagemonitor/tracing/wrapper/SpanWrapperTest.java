package org.stagemonitor.tracing.wrapper;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

import static org.assertj.core.api.Assertions.assertThat;

public class SpanWrapperTest {

	private final MockTracer mockTracer = new MockTracer();
	private final SpanWrappingTracer spanWrappingTracer = new SpanWrappingTracer(mockTracer);

	@Test
	public void testInternalTags() throws Exception {
		spanWrappingTracer.buildSpan("test")
				.withTag(SpanWrapper.INTERNAL_TAG_PREFIX + "foo", "foo")
				.withTag("foo", "foo")
				.start()
				.setTag(SpanWrapper.INTERNAL_TAG_PREFIX + "bar", "bar")
				.setTag("bar", "bar")
				.finish();
		final List<MockSpan> mockSpans = mockTracer.finishedSpans();
		assertThat(mockSpans).hasSize(1);
		final Map<String, Object> tags = mockSpans.get(0).tags();
		assertThat(tags).doesNotContainKeys(SpanWrapper.INTERNAL_TAG_PREFIX + "foo", SpanWrapper.INTERNAL_TAG_PREFIX + "bar");
		assertThat(tags).containsKeys("foo", "bar");
	}
}
