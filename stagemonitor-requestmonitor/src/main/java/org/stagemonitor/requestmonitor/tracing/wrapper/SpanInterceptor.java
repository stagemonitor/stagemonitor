package org.stagemonitor.requestmonitor.tracing.wrapper;

import io.opentracing.Span;
import io.opentracing.Tracer;

public abstract class SpanInterceptor {

	/**
	 * Called when a {@link Span} is started ({@link Tracer.SpanBuilder#start()}
	 * <p/>
	 * Note: when calling <code>spanWrapper.setTag(...)</code>, all registered {@link SpanInterceptor}s are called
	 * again. If you don't want that, call <code>spanWrapper.getDelegate().setTag(...)</code>.
	 * <p/>
	 * Note: tags might be set on the span before it has been started
	 *
	 * @param spanWrapper the span which was just started
	 */
	public void onStart(SpanWrapper spanWrapper) {
	}

	/**
	 * Callback for {@link Span#setTag(String, String)} and {@link Tracer.SpanBuilder#withTag(String, String)}
	 *
	 * This method may be called before a span is started.
	 *
	 * @param key   the tag key
	 * @param value the tag value
	 * @return you can modify the value of the tag by returning a different value then the provided one
	 */
	public String onSetTag(String key, String value) {
		return value;
	}

	/**
	 * Callback for {@link Span#setTag(String, boolean)} and {@link Tracer.SpanBuilder#withTag(String, boolean)}
	 *
	 * This method may be called before a span is started.
	 *
	 * @param key   the tag key
	 * @param value the tag value
	 * @return you can modify the value of the tag by returning a different value then the provided one
	 */
	public boolean onSetTag(String key, boolean value) {
		return value;
	}

	/**
	 * Callback for {@link Span#setTag(String, Number)} and {@link Tracer.SpanBuilder#withTag(String, Number)}
	 *
	 * This method may be called before a span is started.
	 *
	 * @param key   the tag key
	 * @param value the tag value
	 * @return you can modify the value of the tag by returning a different value then the provided one
	 */
	public Number onSetTag(String key, Number value) {
		return value;
	}

	/**
	 * Called when {@link Span#finish} has been called
	 * <p/>
	 * Note: when calling <code>spanWrapper.setTag(...)</code>, all registered {@link SpanInterceptor}s are called
	 * again. If you don't want that, call <code>spanWrapper.getDelegate().setTag(...)</code>.
	 *
	 * @param spanWrapper   the span which has just beed finished
	 * @param operationName the operation name of this span. The operation name is final only on finish because it can
	 *                      be set any time via {@link Span#setOperationName(String)}
	 * @param durationNanos the duration of this span in nanoseconds
	 */
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
	}
}
