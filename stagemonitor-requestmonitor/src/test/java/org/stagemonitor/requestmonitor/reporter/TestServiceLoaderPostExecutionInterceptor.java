package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.requestmonitor.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.sampling.PostExecutionSpanReporterInterceptor;

public class TestServiceLoaderPostExecutionInterceptor extends PostExecutionSpanReporterInterceptor {
	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		context.getSpan().setTag("serviceLoaderWorks", true);
	}
}
