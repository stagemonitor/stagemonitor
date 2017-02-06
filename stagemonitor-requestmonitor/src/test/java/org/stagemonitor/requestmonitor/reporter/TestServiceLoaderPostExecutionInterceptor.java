package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.requestmonitor.sampling.PostExecutionInterceptorContext;
import org.stagemonitor.requestmonitor.sampling.PostExecutionRequestTraceReporterInterceptor;

public class TestServiceLoaderPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {
	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		context.getSpan().setTag("serviceLoaderWorks", true);
	}
}
