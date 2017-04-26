package org.stagemonitor.tracing.sampling;

import com.codahale.metrics.Timer;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.tracing.TracingPlugin;

import java.util.concurrent.TimeUnit;

public class FastExternalSpanExcludingPostExecutionInterceptor extends PostExecutionSpanInterceptor {

	private TracingPlugin tracingPlugin;

	@Override
	public void init(ConfigurationRegistry configuration) {
		tracingPlugin = configuration.getConfig(TracingPlugin.class);
	}

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		if (context.getSpanContext().isExternalRequest()) {
			final double thresholdMs = tracingPlugin.getExcludeExternalRequestsFasterThan();
			final long durationNs = context.getSpanContext().getDurationNanos();
			final long durationMs = TimeUnit.NANOSECONDS.toMillis(context.getSpanContext().getDurationNanos());
			if (durationMs < thresholdMs) {
				context.shouldNotReport(getClass());
				return;
			}

			Timer timer = context.getSpanContext().getTimerForThisRequest();
			final double percentageThreshold = tracingPlugin.getExcludeExternalRequestsWhenFasterThanXPercent();
			if (!MetricUtils.isFasterThanXPercentOfAllRequests(durationNs, percentageThreshold, timer)) {
//			logger.debug("Exclude external request {} because was faster than {}% of all requests",
//					externalRequest.getExecutedBy(), percentageThreshold * 100);
				context.shouldNotReport(getClass());
			}
		}
	}
}
