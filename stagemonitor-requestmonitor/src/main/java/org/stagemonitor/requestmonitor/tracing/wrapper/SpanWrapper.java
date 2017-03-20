package org.stagemonitor.requestmonitor.tracing.wrapper;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.SpanContext;

/**
 * The purpose of this wrapper is to call the registered {@link SpanEventListener}s before certain methods of the
 * {@link #delegate} {@link Span} are invoked.
 */
public class SpanWrapper implements Span {

	/**
	 * The actual span to be invoked
	 */
	private Span delegate;
	private String operationName;
	private final long startTimestampNanos;
	private final List<SpanEventListener> spanEventListeners;

	public SpanWrapper(Span delegate, String operationName, long startTimestampNanos, List<SpanEventListener> spanEventListeners) {
		this.delegate = delegate;
		this.operationName = operationName;
		this.startTimestampNanos = startTimestampNanos;
		this.spanEventListeners = spanEventListeners;
	}

	public SpanContext context() {
		return delegate.context();
	}

	public void close() {
		final long durationNanos = System.nanoTime() - startTimestampNanos;
		for (SpanEventListener spanEventListener : spanEventListeners) {
			spanEventListener.onFinish(this, operationName, durationNanos);
		}
		delegate.close();
	}

	public void finish() {
		final long durationNanos = System.nanoTime() - startTimestampNanos;
		for (SpanEventListener spanEventListener : spanEventListeners) {
			spanEventListener.onFinish(this, operationName, durationNanos);
		}
		delegate.finish();
	}

	public void finish(long finishMicros) {
		final long durationNanos = TimeUnit.MICROSECONDS.toNanos(finishMicros) - startTimestampNanos;
		for (SpanEventListener spanEventListener : spanEventListeners) {
			spanEventListener.onFinish(this, operationName, durationNanos);
		}
		delegate.finish(finishMicros);
	}

	public Span setTag(String key, String value) {
		for (SpanEventListener spanEventListener : spanEventListeners) {
			value = spanEventListener.onSetTag(key, value);
		}
		delegate = delegate.setTag(key, value);
		return this;
	}

	public Span setTag(String key, boolean value) {
		for (SpanEventListener spanEventListener : spanEventListeners) {
			value = spanEventListener.onSetTag(key, value);
		}
		delegate = delegate.setTag(key, value);
		return this;
	}

	public Span setTag(String key, Number value) {
		for (SpanEventListener spanEventListener : spanEventListeners) {
			value = spanEventListener.onSetTag(key, value);
		}
		delegate = delegate.setTag(key, value);
		return this;
	}

	public Span log(String eventName, Object payload) {
		delegate = delegate.log(eventName, payload);
		return this;
	}

	public Span log(long timestampMicroseconds, String eventName, Object payload) {
		delegate = delegate.log(timestampMicroseconds, eventName, payload);
		return this;
	}

	public Span setBaggageItem(String key, String value) {
		delegate = delegate.setBaggageItem(key, value);
		return this;
	}

	public String getBaggageItem(String key) {
		return delegate.getBaggageItem(key);
	}

	public Span setOperationName(String operationName) {
		this.operationName = operationName;
		delegate = delegate.setOperationName(operationName);
		return this;
	}

	@JsonValue
	public Span getDelegate() {
		return delegate;
	}

	public String getOperationName() {
		return operationName;
	}

	public <T extends Span> T unwrap(Class<T> delegateClass) {
		if (delegateClass.isInstance(delegate)) {
			return (T) delegate;
		} else if (delegate instanceof SpanWrapper) {
			return ((SpanWrapper) delegate).unwrap(delegateClass);
		} else {
			return null;
		}
	}
}
