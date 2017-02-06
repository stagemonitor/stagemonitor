package org.stagemonitor.requestmonitor.sampling;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class CallTreeExcludingPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		final double percentileLimit = context
				.getConfig(RequestMonitorPlugin.class)
				.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests();

		if (!MetricUtils.isFasterThanXPercentOfAllRequests(context.getRequestInformation().getDuration(), percentileLimit, context.getRequestInformation().getTimerForThisRequest())) {
			exclude(context);
		}
	}

	private void exclude(PostExecutionInterceptorContext context) {
		context.excludeCallTree();
	}
}
