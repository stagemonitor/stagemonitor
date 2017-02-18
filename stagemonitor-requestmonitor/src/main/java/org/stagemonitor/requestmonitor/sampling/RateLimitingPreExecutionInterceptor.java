package org.stagemonitor.requestmonitor.sampling;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class RateLimitingPreExecutionInterceptor extends PreExecutionSpanReporterInterceptor {

	@Override
	public void interceptReport(PreExecutionInterceptorContext context) {
		final RequestMonitorPlugin config = context.getConfig(RequestMonitorPlugin.class);
		final RequestMonitor.RequestInformation requestInformation = context.getRequestInformation();
		boolean rateExceeded = false;
		if (requestInformation.isExternalRequest()) {
			rateExceeded = MetricUtils.isRateLimitExceeded(config.getOnlyReportNExternalRequestsPerMinute(), context.getExternalRequestReportingRate());
		} else if (requestInformation.isServerRequest()) {
			rateExceeded = MetricUtils.isRateLimitExceeded(config.getOnlyReportNSpansPerMinute(), context.getInternalRequestReportingRate());
		}
		if (rateExceeded) {
			context.shouldNotReport(getClass());
		}
	}

}
