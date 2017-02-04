package org.stagemonitor.requestmonitor.tracing.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public class SpanWrappingTracer implements Tracer {

	private final Tracer delegate;
	private final List<Callable<SpanInterceptor>> spanInterceptorSuppliers;

	public SpanWrappingTracer(Tracer delegate, List<Callable<SpanInterceptor>> spanInterceptorSuppliers) {
		this.delegate = delegate;
		this.spanInterceptorSuppliers = spanInterceptorSuppliers;
	}

	@Override
	public SpanBuilder buildSpan(String operationName) {
		return new SpanWrappingSpanBuilder(delegate.buildSpan(operationName), operationName, createSpanInterceptors());
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
		delegate.inject(spanContext, format, carrier);
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return delegate.extract(format, carrier);
	}

	protected List<SpanInterceptor> createSpanInterceptors() {
		List<SpanInterceptor> spanInterceptors = new ArrayList<SpanInterceptor>(spanInterceptorSuppliers.size());
		for (Callable<SpanInterceptor> spanInterceptorSupplier : spanInterceptorSuppliers) {
			try {
				spanInterceptors.add(spanInterceptorSupplier.call());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return spanInterceptors;
	}

	class SpanWrappingSpanBuilder implements SpanBuilder {

		private final String operationName;
		private final List<SpanInterceptor> spanInterceptors;
		private SpanBuilder delegate;
		private long startTimestampNanos;

		SpanWrappingSpanBuilder(SpanBuilder delegate, String operationName, List<SpanInterceptor> spanInterceptors) {
			this.operationName = operationName;
			this.delegate = delegate;
			this.spanInterceptors = spanInterceptors;
		}

		public Iterable<Map.Entry<String, String>> baggageItems() {
			return delegate.baggageItems();
		}

		public SpanBuilder asChildOf(SpanContext parent) {
			delegate = delegate.asChildOf(parent);
			return this;
		}

		public SpanBuilder asChildOf(Span parent) {
			delegate = delegate.asChildOf(parent);
			return this;
		}

		public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
			delegate = delegate.addReference(referenceType, referencedContext);
			return this;
		}

		public SpanBuilder withTag(String key, String value) {
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				value = spanInterceptor.onSetTag(key, value);
			}
			delegate = delegate.withTag(key, value);
			return this;
		}

		public SpanBuilder withTag(String key, boolean value) {
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				value = spanInterceptor.onSetTag(key, value);
			}
			delegate = delegate.withTag(key, value);
			return this;
		}

		public SpanBuilder withTag(String key, Number value) {
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				value = spanInterceptor.onSetTag(key, value);
			}
			delegate = delegate.withTag(key, value);
			return this;
		}

		public SpanBuilder withStartTimestamp(long microseconds) {
			startTimestampNanos = TimeUnit.MICROSECONDS.toNanos(microseconds);
			delegate = delegate.withStartTimestamp(microseconds);
			return this;
		}

		public Span start() {
			if (startTimestampNanos == 0) {
				startTimestampNanos = System.nanoTime();
			}
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				spanInterceptor.onStart();
			}
			return new SpanWrapper(delegate.start(), operationName, startTimestampNanos, spanInterceptors);
		}
	}
}
