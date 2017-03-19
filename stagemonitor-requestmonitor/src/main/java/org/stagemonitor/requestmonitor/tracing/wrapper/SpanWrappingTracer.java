package org.stagemonitor.requestmonitor.tracing.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public class SpanWrappingTracer implements Tracer {

	private final Tracer delegate;
	private final List<SpanInterceptorFactory> spanInterceptorFactories;

	public SpanWrappingTracer(Tracer delegate) {
		this(delegate, new CopyOnWriteArrayList<SpanInterceptorFactory>());
	}

	public SpanWrappingTracer(Tracer delegate, List<SpanInterceptorFactory> spanInterceptorFactories) {
		this.delegate = delegate;
		this.spanInterceptorFactories = spanInterceptorFactories;
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
		List<SpanInterceptor> spanInterceptors = new ArrayList<SpanInterceptor>(spanInterceptorFactories.size());
		for (SpanInterceptorFactory spanInterceptorFactory : spanInterceptorFactories) {
			spanInterceptors.add(spanInterceptorFactory.create());
		}
		return spanInterceptors;
	}

	public void addSpanInterceptor(SpanInterceptorFactory spanInterceptorFactory) {
		spanInterceptorFactories.add(spanInterceptorFactory);
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
			final SpanWrapper spanWrapper = new SpanWrapper(delegate.start(), operationName, startTimestampNanos, spanInterceptors);
			for (SpanInterceptor spanInterceptor : spanInterceptors) {
				spanInterceptor.onStart(spanWrapper);
			}
			return spanWrapper;
		}
	}
}
