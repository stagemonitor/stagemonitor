package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.metrics.MetricUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.utils.Spans;

import java.util.concurrent.TimeUnit;

class CallTreeExcludingPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {

	private static final String CONTAINS_CALL_TREE = "contains_call_tree";

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		if (!context.getInternalSpan().getTags().containsKey(Spans.CALL_TREE_JSON)) {
			context.getSpan().setTag(CONTAINS_CALL_TREE, false);
			return;
		} else {
			context.getSpan().setTag(CONTAINS_CALL_TREE, true);
		}

		final double percentileLimit = context
				.getConfig(RequestMonitorPlugin.class)
				.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests();

		final long executionTimeNanos = TimeUnit.MICROSECONDS.toNanos(context.getInternalSpan().getDuration());
		if (!MetricUtils.isFasterThanXPercentOfAllRequests(executionTimeNanos, percentileLimit, context.getTimerForThisRequest())) {
			exclude(context);
		}
	}

	private void exclude(PostExecutionInterceptorContext context) {
		context.addExcludedProperties(Spans.CALL_TREE_ASCII, Spans.CALL_TREE_JSON)
				.getSpan().setTag(CONTAINS_CALL_TREE, false);
	}
}
