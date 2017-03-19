package org.stagemonitor.requestmonitor.sampling;

import com.codahale.metrics.Timer;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

import java.util.concurrent.TimeUnit;

public class FastExternalSpanExcludingPostExecutionInterceptor extends PostExecutionSpanInterceptor {

	private RequestMonitorPlugin requestMonitorPlugin;

	@Override
	public void init(Configuration configuration) {
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
	}

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		if (context.getSpanContext().isExternalRequest()) {
			final double thresholdMs = requestMonitorPlugin.getExcludeExternalRequestsFasterThan();
			final long durationNs = context.getSpanContext().getDurationNanos();
			final long durationMs = TimeUnit.NANOSECONDS.toMillis(context.getSpanContext().getDurationNanos());
			if (durationMs < thresholdMs) {
				context.shouldNotReport(getClass());
				return;
			}

			Timer timer = context.getSpanContext().getTimerForThisRequest();
			final double percentageThreshold = requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent();
			if (!MetricUtils.isFasterThanXPercentOfAllRequests(durationNs, percentageThreshold, timer)) {
//			logger.debug("Exclude external request {} because was faster than {}% of all requests",
//					externalRequest.getExecutedBy(), percentageThreshold * 100);
				context.shouldNotReport(getClass());
			}
		}
	}
}
