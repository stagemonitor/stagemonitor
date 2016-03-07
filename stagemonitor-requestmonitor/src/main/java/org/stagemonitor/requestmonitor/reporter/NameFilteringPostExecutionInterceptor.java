package org.stagemonitor.requestmonitor.reporter;

import java.util.Collection;

import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class NameFilteringPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		final Collection<String> onlyReportRequestsWithName = context.getConfig(RequestMonitorPlugin.class)
				.getOnlyReportRequestsWithNameToElasticsearch();
		if (!onlyReportRequestsWithName.isEmpty() && !onlyReportRequestsWithName.contains(context.getRequestTrace().getName())) {
			context.shouldNotReport();
		}
	}

}
