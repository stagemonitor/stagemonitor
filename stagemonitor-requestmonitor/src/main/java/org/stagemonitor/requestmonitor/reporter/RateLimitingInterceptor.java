package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class RateLimitingInterceptor implements ElasticsearchRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(InterceptContext context) {
		final double maxReportingRate = context.getConfig(RequestMonitorPlugin.class)
				.getOnlyReportNRequestsPerMinuteToElasticsearch();

		if (maxReportingRate <= 0) {
			context.shouldNotReport();
		} else if (60 * context.getReportingRate().getOneMinuteRate() > maxReportingRate) {
			context.shouldNotReport();
		}
	}

}
