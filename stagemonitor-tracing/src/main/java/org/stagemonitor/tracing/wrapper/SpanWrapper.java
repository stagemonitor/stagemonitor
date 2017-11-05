package org.stagemonitor.tracing.wrapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.SpanContext;

/**
 * The purpose of this wrapper is to call the registered {@link SpanEventListener}s before certain methods of the
 * {@link #delegate} {@link Span} are invoked.
 */
public class SpanWrapper implements Span {

	/**
	 * Tags which start with this value won't be set on the delegate
	 */
	public static final String INTERNAL_TAG_PREFIX = "internal_";

	private static final double MILLISECOND_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	/**
	 * The actual span to be invoked
	 */
	private Span delegate;
	private String operationName;
	private final long startTimestampNanos;
	private final long startTimestampMillis;
	private final List<SpanEventListener> spanEventListeners;
	private final Map<String, Object> tags;
	private long durationNanos;

	public SpanWrapper(Span delegate, String operationName, long startTimestampNanos, long startTimestampMillis,
					   List<SpanEventListener> spanEventListeners, Map<String, Object> tags) {
		this.delegate = delegate;
		this.operationName = operationName;
		this.startTimestampNanos = startTimestampNanos;
		this.startTimestampMillis = startTimestampMillis;
		this.spanEventListeners = spanEventListeners;
		this.tags = tags;
	}

	@Override
	public SpanContext context() {
		return delegate.context();
	}

	@Override
	public void finish() {
		durationNanos = System.nanoTime() - startTimestampNanos;
		for (SpanEventListener spanEventListener : spanEventListeners) {
			spanEventListener.onFinish(this, operationName, durationNanos);
		}
		delegate.finish();
	}

	@Override
	public void finish(long finishMicros) {
		durationNanos = TimeUnit.MICROSECONDS.toNanos(finishMicros) - startTimestampNanos;
		for (SpanEventListener spanEventListener : spanEventListeners) {
			spanEventListener.onFinish(this, operationName, durationNanos);
		}
		delegate.finish(finishMicros);
	}

	@Override
	public Span setTag(String key, String value) {
		for (SpanEventListener spanEventListener : spanEventListeners) {
			value = spanEventListener.onSetTag(key, value);
		}
		if (value != null) {
			if (!key.startsWith(INTERNAL_TAG_PREFIX)) {
				delegate = delegate.setTag(key, value);
			}
			tags.put(key, value);
		}
		return this;
	}

	@Override
	public Span setTag(String key, boolean value) {
		for (SpanEventListener spanEventListener : spanEventListeners) {
			value = spanEventListener.onSetTag(key, value);
		}
		if (!key.startsWith(INTERNAL_TAG_PREFIX)) {
			delegate = delegate.setTag(key, value);
		}
		tags.put(key, value);
		return this;
	}

	@Override
	public Span setTag(String key, Number value) {
		for (SpanEventListener spanEventListener : spanEventListeners) {
			value = spanEventListener.onSetTag(key, value);
		}
		if (value != null) {
			if (!key.startsWith(INTERNAL_TAG_PREFIX)) {
				delegate = delegate.setTag(key, value);
			}
			tags.put(key, value);
		}
		return this;
	}

	@Override
	public Span log(Map<String, ?> fields) {
		delegate = delegate.log(fields);
		return this;
	}

	@Override
	public Span log(long timestampMicroseconds, Map<String, ?> fields) {
		delegate = delegate.log(timestampMicroseconds, fields);
		return this;
	}

	@Override
	public Span log(String event) {
		delegate = delegate.log(event);
		return this;
	}

	@Override
	public Span log(long timestampMicroseconds, String event) {
		delegate = delegate.log(timestampMicroseconds, event);
		return this;
	}

	@Override
	public Span setBaggageItem(String key, String value) {
		delegate = delegate.setBaggageItem(key, value);
		return this;
	}

	@Override
	public String getBaggageItem(String key) {
		return delegate.getBaggageItem(key);
	}

	@Override
	public Span setOperationName(String operationName) {
		this.operationName = operationName;
		delegate = delegate.setOperationName(operationName);
		return this;
	}

	public Span getDelegate() {
		return delegate;
	}

	public String getOperationName() {
		return operationName;
	}

	public long getStartTimestampMillis() {
		return startTimestampMillis;
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

	public Map<String, Object> getTags() {
		return tags;
	}

	/**
	 * This method returns the tag value, associated with the supplied key, if it exists and has a {@link String} type.
	 *
	 * @param key The tag key
	 * @return The value, if exists and is a {@link String} type, otherwise null
	 */
	public String getStringTag(String key) {
		Object value = tags.get(key);
		if (value instanceof String) {
			return (String) value;
		}
		return null;
	}

	/**
	 * This method returns the tag value, associated with the supplied key, if it exists and has a {@link Number} type.
	 *
	 * @param key The tag key
	 * @return The value, if exists and is a {@link Number} type, otherwise null
	 */
	public Number getNumberTag(String key) {
		Object value = tags.get(key);
		if (value instanceof Number) {
			return (Number) value;
		}
		return null;
	}

	/**
	 * This method returns the tag value, associated with the supplied key, if it exists and has a {@link Boolean}
	 * type.
	 *
	 * @param key The tag key
	 * @return The value, if exists and is a {@link Boolean} type, otherwise null
	 */
	public Boolean getBooleanTag(String key) {
		Object value = tags.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return null;
	}

	public boolean getBooleanTag(String key, boolean defaultIfNull) {
		Object value = tags.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return defaultIfNull;
	}

	public long getDurationNanos() {
		return durationNanos;
	}

	public double getDurationMs() {
		return durationNanos / MILLISECOND_IN_NANOS;
	}

}
