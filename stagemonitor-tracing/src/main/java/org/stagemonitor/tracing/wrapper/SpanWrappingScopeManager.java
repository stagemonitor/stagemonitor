package org.stagemonitor.tracing.wrapper;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;

import java.util.HashMap;
import java.util.Map;

/**
 * The purpose of this class is to make it possible to use implementations of the open tracing api
 * like brave that do not work with the {@link SpanWrapper} class directly.
 */
public class SpanWrappingScopeManager implements ScopeManager, SpanWrappingCallback {

	private ScopeManager delegate;

	private ThreadLocal<Map<String, SpanWrapper>> currentSpanWrapperMapThreadLocal = new ThreadLocal<>();

	public SpanWrappingScopeManager(ScopeManager delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close(String spanId) {
		Map<String, SpanWrapper> spanWrapperMap = currentSpanWrapperMapThreadLocal.get();
		spanWrapperMap.remove(spanId);
		if (spanWrapperMap.isEmpty()) {
			currentSpanWrapperMapThreadLocal.remove();
		}
	}

	@Override
	public Scope activate(Span span) {
		if (span instanceof SpanWrapper) {
			SpanWrapper spanWrapper = (SpanWrapper) span;
			final Span delegate = spanWrapper.getDelegate();
			final String spanId = delegate.context().toSpanId();
			Map<String, SpanWrapper> spanWrapperMap = currentSpanWrapperMapThreadLocal.get();
			if (spanWrapperMap == null) {
				spanWrapperMap = new HashMap<>();
				currentSpanWrapperMapThreadLocal.set(spanWrapperMap);
			}
			spanWrapperMap.put(spanId, spanWrapper);
			return new SpanWrappingScope(this.delegate.activate(delegate), spanId, this);
		}
		return delegate.activate(span);
	}

	@Override
	public Span activeSpan() {
		Span activeSpan = delegate.activeSpan();
		if (activeSpan != null) {
			Map<String, SpanWrapper> spanWrapperMap = currentSpanWrapperMapThreadLocal.get();
			SpanWrapper spanWrapper = spanWrapperMap.get(activeSpan.context().toSpanId());
			if (spanWrapper != null) {
				return spanWrapper;
			}
		}
		return activeSpan;
	}

}
