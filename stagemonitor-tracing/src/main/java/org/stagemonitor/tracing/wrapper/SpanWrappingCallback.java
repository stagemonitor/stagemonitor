package org.stagemonitor.tracing.wrapper;

import io.opentracing.Scope;

/**
 * Callback interface for {@link Scope#close()} in the span wrapping context.
 */
public interface SpanWrappingCallback {

	void close(String spanId);

}
