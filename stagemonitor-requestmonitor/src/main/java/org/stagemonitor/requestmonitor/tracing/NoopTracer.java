package org.stagemonitor.requestmonitor.tracing;

import java.util.Collections;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public final class NoopTracer implements Tracer {

	public static final Tracer INSTANCE = new NoopTracer();

	private NoopTracer() {
	}

	@Override
	public SpanBuilder buildSpan(String operationName) {
		return NoopSpanBuilder.INSTANCE;
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return NoopSpan.NoopSpanContext.INSTANCE;
	}

	private final static class NoopSpanBuilder implements SpanBuilder {

		private static final NoopSpanBuilder INSTANCE = new NoopSpanBuilder();

		@Override
		public SpanBuilder asChildOf(SpanContext parent) {
			return this;
		}

		@Override
		public SpanBuilder asChildOf(Span parent) {
			return this;
		}

		@Override
		public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
			return this;
		}

		@Override
		public SpanBuilder withTag(String key, String value) {
			return this;
		}

		@Override
		public SpanBuilder withTag(String key, boolean value) {
			return this;
		}

		@Override
		public SpanBuilder withTag(String key, Number value) {
			return this;
		}

		@Override
		public SpanBuilder withStartTimestamp(long microseconds) {
			return this;
		}

		@Override
		public Span start() {
			return NoopSpan.INSTANCE;
		}

		@Override
		public Iterable<Map.Entry<String, String>> baggageItems() {
			return Collections.emptyList();
		}
	}

}
