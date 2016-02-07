package org.stagemonitor.requestmonitor.reporter;

import java.util.Collection;

import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class NameFilteringInterceptor implements ElasticsearchRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(InterceptContext context) {
		final Collection<String> onlyReportRequestsWithName = context.getConfig(RequestMonitorPlugin.class)
				.getOnlyReportRequestsWithNameToElasticsearch();
		if (!onlyReportRequestsWithName.isEmpty() && !onlyReportRequestsWithName.contains(context.getRequestTrace().getName())) {
			context.shouldNotReport();
		}
	}

}
