package org.stagemonitor.tracing.elasticsearch.impl;

import org.junit.Test;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StagemonitorPlugin;

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
		final JaegerTracerFactory jaegerTracerFactory = new JaegerTracerFactory();
		final Tracer tracer = jaegerTracerFactory.getTracer(initArguments);
		try (final Scope rootSpan = tracer.buildSpan("foo").startActive()) {
			try (final Scope childSpan = tracer.buildSpan("bar").startActive()) {
				assertThat(jaegerTracerFactory.isRoot(rootSpan.span())).isTrue();
				assertThat(jaegerTracerFactory.isRoot(childSpan.span())).isFalse();
			}
		}
	}
}
