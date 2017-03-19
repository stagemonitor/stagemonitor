package org.stagemonitor.requestmonitor.tracing.wrapper;

public class StatelessSpanInterceptor extends SpanInterceptor implements SpanInterceptorFactory {
	@Override
	public final SpanInterceptor create() {
		return this;
	}
}
