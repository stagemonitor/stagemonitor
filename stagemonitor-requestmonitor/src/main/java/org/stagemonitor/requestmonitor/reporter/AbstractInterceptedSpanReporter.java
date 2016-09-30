package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.requestmonitor.RequestTrace;

import io.opentracing.Span;

public abstract class AbstractInterceptedSpanReporter extends AbstractInterceptedRequestTraceReporter {

	@Override
	protected final <T extends RequestTrace> void doReport(T requestTrace, PostExecutionInterceptorContext context) {
		reportSpan(requestTrace.getSpan(), context);
	}

	abstract void reportSpan(Span span, PostExecutionInterceptorContext context);

}
