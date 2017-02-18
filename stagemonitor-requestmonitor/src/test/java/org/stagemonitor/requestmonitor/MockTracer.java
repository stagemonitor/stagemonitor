package org.stagemonitor.requestmonitor;

import org.stagemonitor.requestmonitor.tracing.NoopSpan;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MockTracer implements Tracer {
	@Override
	public SpanBuilder buildSpan(String operationName) {
		final SpanBuilder spanBuilder = mock(SpanBuilder.class);
		final Span mockSpan = spy(NoopSpan.INSTANCE);
		when(spanBuilder.start()).thenReturn(mockSpan);
		when(spanBuilder.asChildOf(any(SpanContext.class))).thenReturn(spanBuilder);
		when(spanBuilder.asChildOf(any(Span.class))).thenReturn(spanBuilder);
		when(spanBuilder.withStartTimestamp(anyLong())).thenReturn(spanBuilder);
		when(spanBuilder.withTag(any(), anyString()))
				.then(invocation -> {
					mockSpan.setTag(invocation.getArgument(0), invocation.<String>getArgument(1));
					return spanBuilder;
				});
		when(spanBuilder.withTag(any(), any(Number.class)))
				.then(invocation -> {
					mockSpan.setTag(invocation.getArgument(0), invocation.<Number>getArgument(1));
					return spanBuilder;
				});
		when(spanBuilder.withTag(any(), anyBoolean()))
				.then(invocation -> {
					mockSpan.setTag(invocation.getArgument(0), invocation.<Boolean>getArgument(1));
					return spanBuilder;
				});
		return spanBuilder;
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return mock(SpanContext.class);
	}
}
