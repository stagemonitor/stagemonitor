package org.stagemonitor.tracing.wrapper;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;

import java.util.HashMap;
import java.util.Map;

public class SpanWrappingScopeManager implements ScopeManager {

	private ScopeManager delegate;

	private ThreadLocal<Map<String, SpanWrapper>> currentSpanWrapperThreadLocal;

	public SpanWrappingScopeManager(ScopeManager delegate) {
		this.delegate = delegate;
		this.currentSpanWrapperThreadLocal.set(new HashMap<>());
	}

	@Override
	public Scope activate(Span span) {
		if (span instanceof SpanWrapper) {
			SpanWrapper spanWrapper = (SpanWrapper) span;
			Map<String, SpanWrapper> spanWrapperMap = currentSpanWrapperThreadLocal.get();
			spanWrapperMap.put(span.context().toSpanId(), spanWrapper);
			return delegate.activate(spanWrapper.getDelegate());
		}
		return delegate.activate(span);
	}

	@Override
	public Span activeSpan() {
		Span activeSpan = delegate.activeSpan();
		Map<String, SpanWrapper> spanWrapperMap = currentSpanWrapperThreadLocal.get();
		if (spanWrapperMap != null) {
			SpanWrapper spanWrapper = spanWrapperMap.get(activeSpan.context().toSpanId());
			if (spanWrapper != null) {
				return spanWrapper;
			}
		}
		return activeSpan;
	}

}
