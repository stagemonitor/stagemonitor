package org.stagemonitor.tracing.elasticsearch.impl;

import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;

import io.opentracing.Scope;
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
		try (final Scope rootSpan = tracer.buildSpan("foo").startActive()) {
			try (final Scope childSpan = tracer.buildSpan("bar").startActive()) {
				assertThat(SpanUtils.isRoot(tracer, rootSpan.span())).isTrue();
				assertThat(SpanUtils.isRoot(tracer, childSpan.span())).isFalse();
			}
		}
	}
}
