package org.stagemonitor.requestmonitor.tracing.wrapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public class SpanWrappingTracer implements Tracer {

	private final Tracer delegate;

	public SpanWrappingTracer(Tracer delegate) {
		this.delegate = delegate;
	}

	@Override
	public SpanBuilder buildSpan(String operationName) {
		return new SpanWrappingSpanBuilder(delegate.buildSpan(operationName), createSpanInterceptors(operationName));
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
		delegate.inject(spanContext, format, carrier);
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return delegate.extract(format, carrier);
	}

	protected List<SpanInterceptor> createSpanInterceptors(String operationName) {
		return Collections.emptyList();
	}

	class SpanWrappingSpanBuilder implements SpanBuilder {

		private final SpanBuilder delegate;
		private final List<SpanInterceptor> spanInterceptors;

		SpanWrappingSpanBuilder(SpanBuilder delegate, List<SpanInterceptor> spanInterceptors) {
			this.delegate = delegate;
			this.spanInterceptors = spanInterceptors;
		}

		public Iterable<Map.Entry<String, String>> baggageItems() {
			return delegate.baggageItems();
		}

		public SpanBuilder asChildOf(SpanContext parent) {
			return delegate.asChildOf(parent);
		}

		public SpanBuilder asChildOf(Span parent) {
			return delegate.asChildOf(parent);
		}

		public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
			return delegate.addReference(referenceType, referencedContext);
		}

		public SpanBuilder withTag(String key, String value) {
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				value = spanInterceptor.onSetTag(key, value);
			}
			return delegate.withTag(key, value);
		}

		public SpanBuilder withTag(String key, boolean value) {
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				value = spanInterceptor.onSetTag(key, value);
			}
			return delegate.withTag(key, value);
		}

		public SpanBuilder withTag(String key, Number value) {
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				value = spanInterceptor.onSetTag(key, value);
			}
			return delegate.withTag(key, value);
		}

		public SpanBuilder withStartTimestamp(long microseconds) {
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				spanInterceptor.onSetStartTimestamp(TimeUnit.MICROSECONDS.toNanos(microseconds));
			}
			return delegate.withStartTimestamp(microseconds);
		}

		public Span start() {
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				spanInterceptor.onSetStartTimestamp(System.nanoTime());
			}
			return new SpanWrapper(delegate.start(), spanInterceptors);
		}
	}
}
