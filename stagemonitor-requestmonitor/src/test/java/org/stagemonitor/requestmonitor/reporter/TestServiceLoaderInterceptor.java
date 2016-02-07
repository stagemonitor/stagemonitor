package org.stagemonitor.requestmonitor.reporter;

public class TestServiceLoaderInterceptor implements ElasticsearchRequestTraceReporterInterceptor {
	@Override
	public void interceptReport(InterceptContext context) {
		context.addProperty("serviceLoaderWorks", true);
	}
}
