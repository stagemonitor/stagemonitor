package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class RateLimitingPreExecutionInterceptor extends PreExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		final double maxReportingRate = context.getConfig(RequestMonitorPlugin.class)
				.getOnlyReportNRequestsPerMinuteToElasticsearch();

		if (MetricUtils.isRateLimitExceeded(maxReportingRate, context.getReportingRate())) {
			context.shouldNotReport(getClass());
		}
	}

}
