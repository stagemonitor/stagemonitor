package org.stagemonitor.requestmonitor.tracing.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

/**
 * The purpose of this class is to make it possible to register {@link SpanEventListener}s which are created by
 * {@link SpanEventListenerFactory}s. The {@link SpanEventListener}s are called when certain methods of a
 * {@link SpanBuilder} or {@link Span} are called.
 */
public class SpanWrappingTracer implements Tracer {

	private final Tracer delegate;
	private final Collection<SpanEventListenerFactory> spanInterceptorFactories;

	public SpanWrappingTracer(Tracer delegate) {
		this(delegate, new CopyOnWriteArrayList<SpanEventListenerFactory>());
	}

	public SpanWrappingTracer(Tracer delegate, Collection<SpanEventListenerFactory> spanInterceptorFactories) {
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

	protected List<SpanEventListener> createSpanInterceptors() {
		List<SpanEventListener> spanEventListeners = new ArrayList<SpanEventListener>(spanInterceptorFactories.size());
		for (SpanEventListenerFactory spanEventListenerFactory : spanInterceptorFactories) {
			spanEventListeners.add(spanEventListenerFactory.create());
		}
		return spanEventListeners;
	}

	public void addSpanInterceptor(SpanEventListenerFactory spanEventListenerFactory) {
		spanInterceptorFactories.add(spanEventListenerFactory);
	}

	class SpanWrappingSpanBuilder implements SpanBuilder {

		private final String operationName;
		private final List<SpanEventListener> spanEventListeners;
		private SpanBuilder delegate;
		private long startTimestampNanos;

		SpanWrappingSpanBuilder(SpanBuilder delegate, String operationName, List<SpanEventListener> spanEventListeners) {
			this.operationName = operationName;
			this.delegate = delegate;
			this.spanEventListeners = spanEventListeners;
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
			for (SpanEventListener spanEventListener : spanEventListeners) {
				value = spanEventListener.onSetTag(key, value);
			}
			delegate = delegate.withTag(key, value);
			return this;
		}

		public SpanBuilder withTag(String key, boolean value) {
			for (SpanEventListener spanEventListener : spanEventListeners) {
				value = spanEventListener.onSetTag(key, value);
			}
			delegate = delegate.withTag(key, value);
			return this;
		}

		public SpanBuilder withTag(String key, Number value) {
			for (SpanEventListener spanEventListener : spanEventListeners) {
				value = spanEventListener.onSetTag(key, value);
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
			final SpanWrapper spanWrapper = new SpanWrapper(delegate.start(), operationName, startTimestampNanos, spanEventListeners);
			for (SpanEventListener spanEventListener : spanEventListeners) {
				spanEventListener.onStart(spanWrapper);
			}
			return spanWrapper;
		}
	}
}
