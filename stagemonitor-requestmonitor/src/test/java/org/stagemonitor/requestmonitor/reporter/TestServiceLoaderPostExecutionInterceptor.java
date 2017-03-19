package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.requestmonitor.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.sampling.PostExecutionSpanInterceptor;

public class TestServiceLoaderPostExecutionInterceptor extends PostExecutionSpanInterceptor {
	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		context.getSpan().setTag("serviceLoaderWorks", true);
	}
}
