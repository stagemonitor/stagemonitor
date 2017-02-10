package org.stagemonitor.requestmonitor.sampling;

import com.codahale.metrics.Timer;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.concurrent.TimeUnit;

public class FastExternalSpanExcludingPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		if (context.getRequestInformation().isExternalRequest()) {
			final RequestMonitorPlugin requestMonitorPlugin = context.getConfig(RequestMonitorPlugin.class);
			final double thresholdMs = requestMonitorPlugin.getExcludeExternalRequestsFasterThan();
			final long durationNs = context.getRequestInformation().getDurationNanos();
			final long durationMs = TimeUnit.NANOSECONDS.toMillis(context.getRequestInformation().getDurationNanos());
			if (durationMs < thresholdMs) {
				context.shouldNotReport(getClass());
				return;
			}

			Timer timer = context.getRequestInformation().getTimerForThisRequest();
			final double percentageThreshold = requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent();
			if (!MetricUtils.isFasterThanXPercentOfAllRequests(durationNs, percentageThreshold, timer)) {
//			logger.debug("Exclude external request {} because was faster than {}% of all requests",
//					externalRequest.getExecutedBy(), percentageThreshold * 100);
				context.shouldNotReport(getClass());
			}
		}
	}
}
