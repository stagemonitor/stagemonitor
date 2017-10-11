package org.stagemonitor.tracing.elasticsearch.impl;

import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;

import io.opentracing.Span;
import io.opentracing.Tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JaegerTracerFactoryTest {

	@Test
	public void testIsRoot() throws Exception {
		final StagemonitorPlugin.InitArguments initArguments = mock(StagemonitorPlugin.InitArguments.class);
		when(initArguments.getMeasurementSession()).thenReturn(
				new MeasurementSession("JaegerTracerFactoryTest", "test", "test"));
		final Tracer tracer = new JaegerTracerFactory().getTracer(initArguments);
		try (final Span rootSpan = tracer.buildSpan("foo").start()) {
			try (final Span childSpan = tracer.buildSpan("bar").asChildOf(rootSpan).start()) {
				assertThat(SpanUtils.isRoot(tracer, rootSpan)).isTrue();
				assertThat(SpanUtils.isRoot(tracer, childSpan)).isFalse();
			}
		}
	}
}
