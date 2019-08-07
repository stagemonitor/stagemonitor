package org.stagemonitor.tracing.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.stagemonitor.tracing.wrapper.SpanWrapper.INTERNAL_TAG_PREFIX;

/**
 * The purpose of this class is to make it possible to register {@link SpanEventListener}s which are created by {@link
 * SpanEventListenerFactory}s. The {@link SpanEventListener}s are called when certain methods of a {@link SpanBuilder}
 * or {@link Span} are called.
 */
public class SpanWrappingTracer implements Tracer {

	private static final Logger logger = LoggerFactory.getLogger(SpanWrappingTracer.class);

	private final Tracer delegate;
	private final Collection<SpanEventListenerFactory> spanInterceptorFactories = new CopyOnWriteArrayList<SpanEventListenerFactory>();

	public SpanWrappingTracer(Tracer delegate) {
		this(delegate, new CopyOnWriteArrayList<SpanEventListenerFactory>());
	}

	public SpanWrappingTracer(Tracer delegate, Collection<SpanEventListenerFactory> spanInterceptorFactories) {
		this.delegate = delegate;
		this.spanInterceptorFactories.addAll(spanInterceptorFactories);
	}

	@Override
	public ScopeManager scopeManager() {
		return delegate.scopeManager();
	}

    @Override
    public Span activeSpan() {
        return scopeManager().activeSpan();
    }

	@Override
	public Scope activateSpan(Span span) {
		return delegate.activateSpan(span);
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public SpanWrappingSpanBuilder buildSpan(String operationName) {
		return new SpanWrappingSpanBuilder(delegate.buildSpan(operationName), operationName, createSpanInterceptors());
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
		delegate.inject(spanContext, format, carrier);
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C carrier) {
		try {
			return delegate.extract(format, carrier);
		} catch (NumberFormatException e) {
			logger.error("error parsing span id, possible XSS attack: " + e.getMessage(), e);
			return null;
		}
	}

	protected List<SpanEventListener> createSpanInterceptors() {
		List<SpanEventListener> spanEventListeners = new ArrayList<SpanEventListener>(spanInterceptorFactories.size());
		for (SpanEventListenerFactory spanEventListenerFactory : spanInterceptorFactories) {
			spanEventListeners.add(spanEventListenerFactory.create());
		}
		return spanEventListeners;
	}

	public void addEventListenerFactory(SpanEventListenerFactory spanEventListenerFactory) {
		spanInterceptorFactories.add(spanEventListenerFactory);
	}

	/**
	 * @throws java.util.ConcurrentModificationException this exception might be thrown if the {@link Collection}
	 *                                                   implementation of {@link #spanInterceptorFactories} does not
	 *                                                   support concurrent access and.
	 */
	public boolean removeEventListenerFactory(SpanEventListenerFactory spanEventListenerFactory) {
		return spanInterceptorFactories.remove(spanEventListenerFactory);
	}

	public class SpanWrappingSpanBuilder implements SpanBuilder {

		private final String operationName;
		private final List<SpanEventListener> spanEventListeners;
		private final ConcurrentMap<String, Object> tags = new ConcurrentHashMap<String, Object>();
		private SpanBuilder delegate;
		private long startTimestampNanos;
		private long startTimestampMillis;

		SpanWrappingSpanBuilder(SpanBuilder delegate, String operationName, List<SpanEventListener> spanEventListeners) {
			this.operationName = operationName;
			this.delegate = delegate;
			this.spanEventListeners = spanEventListeners;
		}

		public SpanWrappingSpanBuilder asChildOf(SpanContext parent) {
			delegate = delegate.asChildOf(parent);
			return this;
		}

		public SpanWrappingSpanBuilder asChildOf(Span parent) {
			delegate = delegate.asChildOf(parent);
			return this;
		}

		public SpanWrappingSpanBuilder addReference(String referenceType, SpanContext referencedContext) {
			delegate = delegate.addReference(referenceType, referencedContext);
			return this;
		}

		@Override
		public SpanBuilder ignoreActiveSpan() {
			delegate = delegate.ignoreActiveSpan();
			return this;
		}

		@Override
		public <T> SpanBuilder withTag(Tag<T> tag, T value) {
			final String key = tag.getKey();
			for (SpanEventListener spanEventListener : spanEventListeners) {
				value = spanEventListener.onSetTag(tag, value);
			}
			if (value != null) {
				if (!key.startsWith(INTERNAL_TAG_PREFIX)) {
					delegate = delegate.withTag(tag, value);
				}
				tags.put(key, value);
			}
			return this;
		}

		public SpanWrappingSpanBuilder withTag(String key, String value) {
			for (SpanEventListener spanEventListener : spanEventListeners) {
				value = spanEventListener.onSetTag(key, value);
			}
			if (value != null) {
				if (!key.startsWith(INTERNAL_TAG_PREFIX)) {
					delegate = delegate.withTag(key, value);
				}
				tags.put(key, value);
			}
			return this;
		}

		public SpanWrappingSpanBuilder withTag(String key, boolean value) {
			for (SpanEventListener spanEventListener : spanEventListeners) {
				value = spanEventListener.onSetTag(key, value);
			}
			if (!key.startsWith(INTERNAL_TAG_PREFIX)) {
				delegate = delegate.withTag(key, value);
			}
			tags.put(key, value);
			return this;
		}

		public SpanWrappingSpanBuilder withTag(String key, Number value) {
			for (SpanEventListener spanEventListener : spanEventListeners) {
				value = spanEventListener.onSetTag(key, value);
			}
			if (value != null) {
				if (!key.startsWith(INTERNAL_TAG_PREFIX)) {
					delegate = delegate.withTag(key, value);
				}
				tags.put(key, value);
			}
			return this;
		}

		public SpanWrappingSpanBuilder withStartTimestamp(long microseconds) {
			startTimestampNanos = TimeUnit.MICROSECONDS.toNanos(microseconds);
			startTimestampMillis = TimeUnit.MICROSECONDS.toMillis(microseconds);
			delegate = delegate.withStartTimestamp(microseconds);
			return this;
		}

		@Override
		public SpanWrapper start() {
			return startSpanWrapper(delegate.start());
		}

		private SpanWrapper startSpanWrapper(Span span) {
			if (startTimestampNanos == 0) {
				startTimestampNanos = System.nanoTime();
				startTimestampMillis = System.currentTimeMillis();
			}
			final SpanWrapper spanWrapper = new SpanWrapper(span, operationName, startTimestampNanos, startTimestampMillis, spanEventListeners, tags);
			for (SpanEventListener spanEventListener : spanEventListeners) {
				spanEventListener.onStart(spanWrapper);
			}
			return spanWrapper;
		}
	}
}
