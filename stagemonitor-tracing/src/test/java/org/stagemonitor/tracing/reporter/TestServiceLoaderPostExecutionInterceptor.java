package org.stagemonitor.tracing.reporter;

import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.tracing.sampling.PostExecutionSpanInterceptor;

public class TestServiceLoaderPostExecutionInterceptor extends PostExecutionSpanInterceptor {
	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		TracingPlugin.getCurrentSpan().setTag("serviceLoaderWorks", true);
	}
}
