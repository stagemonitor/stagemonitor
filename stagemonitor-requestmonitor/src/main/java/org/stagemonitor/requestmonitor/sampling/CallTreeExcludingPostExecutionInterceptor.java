package org.stagemonitor.requestmonitor.sampling;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class CallTreeExcludingPostExecutionInterceptor extends PostExecutionSpanInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		final double percentileLimit = context
				.getConfig(RequestMonitorPlugin.class)
				.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests();

		if (!MetricUtils.isFasterThanXPercentOfAllRequests(context.getSpanContext().getDurationNanos(), percentileLimit, context.getSpanContext().getTimerForThisRequest())) {
			context.excludeCallTree();
		}
	}

}
