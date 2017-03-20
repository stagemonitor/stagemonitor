package org.stagemonitor.requestmonitor.tracing.wrapper;

import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * Provides callbacks for interesting events triggered by calling certain {@link Span} and {@link
 * io.opentracing.Tracer.SpanBuilder} methods.
 * <p/>
 * Use Cases:
 * <ul>
 * <li>
 * Tracking of metrics in {@link #onFinish(SpanWrapper, String, long)} based on the operation name, the
 * duration and possibly some tags which are stored in a class variable.
 * </li>
 * <li>
 * Context propagation: set the current span in {@link #onStart(SpanWrapper)} for example into a {@link ThreadLocal}
 * {@link java.util.Stack} and pop in in {@link #onFinish(SpanWrapper, String, long)}
 * </li>
 * <li>
 * Populate the {@link org.slf4j.MDC}
 * </li>
 * <li>
 * Implement custom span reporters by leveraging the {@link #onFinish(SpanWrapper, String, long)} method
 * </li>
 * </ul>
 */
public interface SpanEventListener {
	/**
	 * Called when a {@link Span} is started ({@link Tracer.SpanBuilder#start()}
	 * <p/>
	 * Note: when calling <code>spanWrapper.setTag(...)</code>, all registered {@link SpanEventListener}s are notified.
	 * If you don't want that, call <code>spanWrapper.getDelegate().setTag(...)</code>.
	 * <p/>
	 * Note: tags might be set on the span before it has been started
	 *
	 * @param spanWrapper the span which was just started
	 */
	void onStart(SpanWrapper spanWrapper);

	/**
	 * Callback for {@link Span#setTag(String, String)} and {@link Tracer.SpanBuilder#withTag(String, String)}
	 *
	 * This method may be called before a span is started.
	 *
	 * @param key   the tag key
	 * @param value the tag value
	 * @return you can modify the value of the tag by returning a different value then the provided one
	 */
	String onSetTag(String key, String value);

	/**
	 * Callback for {@link Span#setTag(String, boolean)} and {@link Tracer.SpanBuilder#withTag(String, boolean)}
	 *
	 * This method may be called before a span is started.
	 *
	 * @param key   the tag key
	 * @param value the tag value
	 * @return you can modify the value of the tag by returning a different value then the provided one
	 */
	boolean onSetTag(String key, boolean value);

	/**
	 * Callback for {@link Span#setTag(String, Number)} and {@link Tracer.SpanBuilder#withTag(String, Number)}
	 *
	 * This method may be called before a span is started.
	 *
	 * @param key   the tag key
	 * @param value the tag value
	 * @return you can modify the value of the tag by returning a different value then the provided one
	 */
	Number onSetTag(String key, Number value);

	/**
	 * Callback for {@link Span#finish}. The actual span will be finished after all {@link #onFinish} callbacks have
	 * been executed.
	 * <p/>
	 * Note: when calling <code>spanWrapper.setTag(...)</code>, all registered {@link SpanEventListener}s are notified.
	 * If you don't want that, call <code>spanWrapper.getDelegate().setTag(...)</code>.
	 *
	 * @param spanWrapper   the span which has just beed finished
	 * @param operationName the operation name of this span. The operation name is final only on finish because it can
	 *                      be set any time via {@link Span#setOperationName(String)}
	 * @param durationNanos the duration of this span in nanoseconds
	 */
	void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos);
}
