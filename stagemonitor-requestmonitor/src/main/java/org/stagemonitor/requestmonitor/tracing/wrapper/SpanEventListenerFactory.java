package org.stagemonitor.requestmonitor.tracing.wrapper;

import io.opentracing.Span;

/**
 * As {@link SpanEventListener}s can be stateful, for example to store certain tags in a instance variable,
 * a new instance of {@link SpanEventListener} has to be created for each individual {@link Span}.
 * <p/>
 * If your {@link SpanEventListener} is stateless i.e. it does not use instance variables which are dependent on the
 * parameters of any {@link SpanEventListener} method, use {@link StatelessSpanEventListener} as a base class as it
 * reuses the same instance for each {@link Span}.
 */
public interface SpanEventListenerFactory {
	SpanEventListener create();
}
