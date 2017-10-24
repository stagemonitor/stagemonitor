package org.stagemonitor.tracing.tagexclude;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.tracing.TracingPlugin;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TagExcludingSpanEventListenerTest {

	private TagExcludingSpanEventListener tagExcludingSpanEventListener;
	private TracingPlugin tracingPlugin;

	@Before
	public void setUp() throws Exception {
		tracingPlugin = mock(TracingPlugin.class);
		tagExcludingSpanEventListener = new TagExcludingSpanEventListener(tracingPlugin);
	}

	@Test
	public void testNoExclude() throws Exception {
		assertThat(tagExcludingSpanEventListener.onSetTag("foo", "bar")).isEqualTo("bar");
		assertThat(tagExcludingSpanEventListener.onSetTag("foo", 1)).isEqualTo(1);
	}

	@Test
	public void testOtherExclude() throws Exception {
		when(tracingPlugin.getExcludedTags()).thenReturn(Collections.singleton("bar"));
		assertThat(tagExcludingSpanEventListener.onSetTag("foo", "bar")).isEqualTo("bar");
		assertThat(tagExcludingSpanEventListener.onSetTag("foo", 1)).isEqualTo(1);
	}

	@Test
	public void testExclude() throws Exception {
		when(tracingPlugin.getExcludedTags()).thenReturn(Collections.singleton("foo"));
		assertThat(tagExcludingSpanEventListener.onSetTag("foo", "bar")).isNull();
		assertThat(tagExcludingSpanEventListener.onSetTag("foo", 1)).isNull();
	}
}
