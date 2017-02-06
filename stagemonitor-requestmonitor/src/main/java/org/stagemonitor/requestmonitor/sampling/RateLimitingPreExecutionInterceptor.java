package org.stagemonitor.requestmonitor.sampling;

import com.codahale.metrics.Meter;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class RateLimitingPreExecutionInterceptor extends PreExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		final RequestMonitorPlugin config = context.getConfig(RequestMonitorPlugin.class);
		final double maxReportingRate;
		final Meter rate;
		if (context.getRequestInformation().isExternalRequest()) {
			maxReportingRate = config.getOnlyReportNExternalRequestsPerMinute();
			rate = context.getExternalRequestReportingRate();
		} else {
			maxReportingRate = config.getOnlyReportNRequestsPerMinuteToElasticsearch();
			rate = context.getInternalRequestReportingRate();
		}
		if (MetricUtils.isRateLimitExceeded(maxReportingRate, rate)) {
			context.shouldNotReport(getClass());
		}

	}

}
