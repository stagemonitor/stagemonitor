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
		when(spanBuilder.withTag(any(), anyString())).thenReturn(spanBuilder);
		when(spanBuilder.withTag(any(), any(Number.class))).thenReturn(spanBuilder);
		when(spanBuilder.withTag(any(), anyBoolean())).thenReturn(spanBuilder);
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
