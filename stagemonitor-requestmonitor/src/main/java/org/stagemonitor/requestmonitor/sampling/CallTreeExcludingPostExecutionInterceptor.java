package org.stagemonitor.requestmonitor.sampling;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class CallTreeExcludingPostExecutionInterceptor extends PostExecutionSpanReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		final double percentileLimit = context
				.getConfig(RequestMonitorPlugin.class)
				.getExcludeCallTreeFromReportWhenFasterThanXPercentOfRequests();

		if (!MetricUtils.isFasterThanXPercentOfAllRequests(context.getRequestInformation().getDurationNanos(), percentileLimit, context.getRequestInformation().getTimerForThisRequest())) {
			exclude(context);
		}
	}

	private void exclude(PostExecutionInterceptorContext context) {
		context.excludeCallTree();
	}
}
