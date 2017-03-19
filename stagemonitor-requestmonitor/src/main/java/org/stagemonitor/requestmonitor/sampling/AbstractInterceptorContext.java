package org.stagemonitor.requestmonitor.sampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.requestmonitor.SpanContextInformation;

import io.opentracing.Span;

public class AbstractInterceptorContext<T extends AbstractInterceptorContext> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractInterceptorContext.class);

	private final SpanContextInformation spanContext;
	private boolean mustReport = false;
	private boolean report = true;

	public AbstractInterceptorContext(SpanContextInformation spanContext) {
		this.spanContext = spanContext;
	}

	public T mustReport(Class<?> interceptorClass) {
		logger.debug("Must report current span (requested by {})", interceptorClass);
		mustReport = true;
		report = true;
		return (T) this;
	}

	public T shouldNotReport(Class<?> interceptorClass) {
		logger.debug("Should not report current span (requested by {})", interceptorClass);
		if (!mustReport) {
			report = false;
		}
		return (T) this;
	}

	public boolean isReport() {
		return report;
	}


	public Span getSpan() {
		return spanContext.getSpan();
	}

	public SpanContextInformation getSpanContext() {
		return spanContext;
	}

}
