package org.stagemonitor.tracing.sampling;

public class PercentSamplingPreExecutionInterceptor extends PreExecutionSpanInterceptor {
	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		context.shouldNotReport(getClass());
	}
}
