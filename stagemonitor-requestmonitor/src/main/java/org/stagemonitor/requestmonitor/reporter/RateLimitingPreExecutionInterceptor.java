package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class RateLimitingPreExecutionInterceptor extends PreExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		final double maxReportingRate = context.getConfig(RequestMonitorPlugin.class)
				.getOnlyReportNRequestsPerMinuteToElasticsearch();

		if (maxReportingRate <= 0) {
			context.shouldNotReport();
		} else if (60 * context.getReportingRate().getOneMinuteRate() > maxReportingRate) {
			context.shouldNotReport();
		}
	}

}
