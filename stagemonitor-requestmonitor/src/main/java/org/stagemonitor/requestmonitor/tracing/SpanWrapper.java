package org.stagemonitor.requestmonitor.tracing;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.SpanContext;

public class SpanWrapper implements Span {

	private final Span delegate;
	private final List<SpanInterceptor> spanInterceptors;

	public SpanWrapper(Span delegate, List<SpanInterceptor> spanInterceptors) {
		this.delegate = delegate;
		this.spanInterceptors = spanInterceptors;
	}

	public SpanContext context() {
		return delegate.context();
	}

	public void close() {
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			spanInterceptor.onFinish(System.nanoTime());
		}
		delegate.close();
	}

	public void finish() {
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			spanInterceptor.onFinish(System.nanoTime());
		}
		delegate.finish();
	}

	public void finish(long finishMicros) {
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			spanInterceptor.onFinish(TimeUnit.MICROSECONDS.toNanos(finishMicros));
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
		for (SpanInterceptor spanInterceptor : spanInterceptors) {
			spanInterceptor.onSetOperationName(operationName);
		}
		return delegate.setOperationName(operationName);
	}

}
