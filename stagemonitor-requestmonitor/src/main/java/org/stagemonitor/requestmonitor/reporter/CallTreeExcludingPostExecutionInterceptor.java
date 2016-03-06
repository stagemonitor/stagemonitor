package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Timer;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;

class CallTreeExcludingPostExecutionInterceptor implements PostExecutionRequestTraceReporterInterceptor {

	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		if (context.getRequestTrace().getCallStack() == null) {
			context.addProperty("containsCallTree", false);
			return;
		} else {
			context.addProperty("containsCallTree", true);
		}

		final double percentileLimit = context
				.getConfig(RequestMonitorPlugin.class)
				.getExcludeCallTreeFromElasticsearchReportWhenFasterThanXPercentOfRequests();

		if (percentileLimit > 0) {
			if (percentileLimit >= 1) {
				exclude(context);
			} else {
				final Timer timer = context.getTimerForThisRequest();
				if (timer != null) {
					final double percentile = timer.getSnapshot().getValue(percentileLimit);
					final long executionTime = context.getRequestTrace().getExecutionTime();
					if (executionTime < percentile) {
						exclude(context);
					}
				}
			}
		}
	}

	private void exclude(PostExecutionInterceptorContext context) {
		context.addExcludedProperties("callStack", "callStackJson").addProperty("containsCallTree", false);
	}
}
