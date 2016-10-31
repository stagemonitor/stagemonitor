package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.Collection;

class NameFilteringPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		final Collection<String> onlyReportRequestsWithName = context.getConfig(RequestMonitorPlugin.class)
				.getOnlyReportRequestsWithNameToElasticsearch();
		if (!onlyReportRequestsWithName.isEmpty() && !onlyReportRequestsWithName.contains(context.getInternalSpan().getOperationName())) {
			context.shouldNotReport(getClass());
		}
	}

}
