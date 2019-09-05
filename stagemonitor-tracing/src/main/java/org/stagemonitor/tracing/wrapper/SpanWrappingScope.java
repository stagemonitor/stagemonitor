package org.stagemonitor.tracing.wrapper;

import io.opentracing.Scope;

/**
 * The purpose of this class is to make it possible to use implementations of the open tracing api
 * like brave that do not work with the {@link SpanWrapper} class directly.
 */
public class SpanWrappingScope implements Scope {

	private SpanWrappingCallback spanWrappingCallback;

	private String spanId;

	private Scope delegate;

	public SpanWrappingScope(Scope delegate, String spanId, SpanWrappingCallback spanWrappingCallback) {
		this.delegate = delegate;
		this.spanId = spanId;
		this.spanWrappingCallback = spanWrappingCallback;
	}

	@Override
	public void close() {
		spanWrappingCallback.close(spanId);
		this.delegate.close();
	}
}
