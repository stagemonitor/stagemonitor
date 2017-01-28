package org.stagemonitor.requestmonitor.tracing.wrapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.SpanContext;

public class SpanWrapper implements Span {

	private final Span delegate;
	private String operationName;
	private final long startTimestampNanos;
	private final List<SpanInterceptor> spanInterceptors;

	public SpanWrapper(Span delegate, String operationName, long startTimestampNanos, List<SpanInterceptor> spanInterceptors) {
		this.delegate = delegate;
		this.operationName = operationName;
		this.startTimestampNanos = startTimestampNanos;
		this.spanInterceptors = spanInterceptors;
	}

	public SpanContext context() {
		return delegate.context();
	}

	public void close() {
		final long durationNanos = System.nanoTime() - startTimestampNanos;
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			spanInterceptor.onFinish(delegate, operationName, durationNanos);
		}
		delegate.close();
	}

	public void finish() {
		final long durationNanos = System.nanoTime() - startTimestampNanos;
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			spanInterceptor.onFinish(delegate, operationName, durationNanos);
		}
		delegate.finish();
	}

	public void finish(long finishMicros) {
		final long durationNanos = TimeUnit.MICROSECONDS.toNanos(finishMicros) - startTimestampNanos;
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			spanInterceptor.onFinish(delegate, operationName, durationNanos);
		}
		delegate.finish(finishMicros);
	}

	public Span setTag(String key, String value) {
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			value = spanInterceptor.onSetTag(key, value);
		}
		return delegate.setTag(key, value);
	}

	public Span setTag(String key, boolean value) {
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			value = spanInterceptor.onSetTag(key, value);
		}
		return delegate.setTag(key, value);
	}

	public Span setTag(String key, Number value) {
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			value = spanInterceptor.onSetTag(key, value);
		}
		return delegate.setTag(key, value);
	}

	public Span log(String eventName, Object payload) {
		return delegate.log(eventName, payload);
	}

	public Span log(long timestampMicroseconds, String eventName, Object payload) {
		return delegate.log(timestampMicroseconds, eventName, payload);
	}

	public Span setBaggageItem(String key, String value) {
		return delegate.setBaggageItem(key, value);
	}

	public String getBaggageItem(String key) {
		return delegate.getBaggageItem(key);
	}

	public Span setOperationName(String operationName) {
		this.operationName = operationName;
		return delegate.setOperationName(operationName);
	}

	public Span getDelegate() {
		return delegate;
	}
}
