package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class CallTreeExcludingPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		if (context.getRequestTrace() == null) {
			// TODO
			return;
		}

		if (context.getRequestTrace().getCallStack() == null) {
			context.getSpan().setTag("containsCallTree", false);
			return;
		} else {
			context.getSpan().setTag("containsCallTree", true);
		}

		final double percentileLimit = context
				.getConfig(RequestMonitorPlugin.class)
				.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests();

		if (!MetricUtils.isFasterThanXPercentOfAllRequests(context.getRequestTrace().getExecutionTime(),
				percentileLimit, context.getTimerForThisRequest())) {
			exclude(context);
		}
	}

	private void exclude(PostExecutionInterceptorContext context) {
		context.addExcludedProperties("callStack", "callStackJson", "callTreeAscii", "callTreeJson").getSpan().setTag("containsCallTree", false);
	}
}
