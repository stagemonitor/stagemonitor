package org.stagemonitor.tracing.reporter;

import org.stagemonitor.tracing.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.PostExecutionSpanInterceptor;

public class TestServiceLoaderPostExecutionInterceptor extends PostExecutionSpanInterceptor {
	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		context.getSpan().setTag("serviceLoaderWorks", true);
	}
}
