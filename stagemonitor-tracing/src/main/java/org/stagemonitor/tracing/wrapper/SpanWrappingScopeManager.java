package org.stagemonitor.tracing.wrapper;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;

import java.util.HashMap;
import java.util.Map;

public class SpanWrappingScopeManager implements ScopeManager {

	private ScopeManager delegate;

	private ThreadLocal<Map<Span, SpanWrapper>> currentSpanWrapperThreadLocal = new ThreadLocal<>();

	public SpanWrappingScopeManager(ScopeManager delegate) {
		this.delegate = delegate;
		this.currentSpanWrapperThreadLocal.set(new HashMap<>());
	}

	@Override
	public Scope activate(Span span) {
		if (span instanceof SpanWrapper) {
			SpanWrapper spanWrapper = (SpanWrapper) span;
			Map<Span, SpanWrapper> spanWrapperMap = currentSpanWrapperThreadLocal.get();
			if (spanWrapperMap == null) {
				spanWrapperMap = new HashMap<>();
				currentSpanWrapperThreadLocal.set(spanWrapperMap);
			}
			System.out.println("put" + span);
			spanWrapperMap.put(span, spanWrapper);
			return delegate.activate(spanWrapper.getDelegate());
		}
		return delegate.activate(span);
	}

	@Override
	public Span activeSpan() {
		Span activeSpan = delegate.activeSpan();
		Map<Span, SpanWrapper> spanWrapperMap = currentSpanWrapperThreadLocal.get();
		if (spanWrapperMap != null) {
			System.out.println("get " + activeSpan);
			SpanWrapper spanWrapper = spanWrapperMap.get(activeSpan);
			if (spanWrapper != null) {
				System.out.println("return span wrapper");
				return spanWrapper;
			}
		}
		System.out.println("return active span");
		return activeSpan;
	}

}
