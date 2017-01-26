package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Timer;
import com.uber.jaeger.Span;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.concurrent.TimeUnit;

import static org.stagemonitor.requestmonitor.reporter.ExternalRequestMetricsReporter.getExternalRequestTimerName;

public class FastExecutionExclustionPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		final Span internalSpan = context.getInternalSpan();
		if (SpanUtils.isExternalRequest(internalSpan)) {
			final RequestMonitorPlugin requestMonitorPlugin = context.getConfig(RequestMonitorPlugin.class);
			final double thresholdMs = requestMonitorPlugin.getExcludeExternalRequestsFasterThan();
			final long durationMs = TimeUnit.MICROSECONDS.toMillis(context.getReportArguments().getDuration());
			final long durationNs = TimeUnit.MICROSECONDS.toNanos(context.getReportArguments().getDuration());
			if (durationMs < thresholdMs) {
				context.shouldNotReport(getClass());
				return;
			}

			Timer timer = context.getConfig(CorePlugin.class).getMetricRegistry().timer(getExternalRequestTimerName(internalSpan));
			final double percentageThreshold = requestMonitorPlugin.getExcludeExternalRequestsWhenFasterThanXPercent();
			if (!MetricUtils.isFasterThanXPercentOfAllRequests(durationNs, percentageThreshold, timer)) {
//			logger.debug("Exclude external request {} because was faster than {}% of all requests",
//					externalRequest.getExecutedBy(), percentageThreshold * 100);
				context.shouldNotReport(getClass());
			}
		}
	}
}
