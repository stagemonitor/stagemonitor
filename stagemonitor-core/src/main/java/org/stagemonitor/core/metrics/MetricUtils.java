package org.stagemonitor.core.metrics;

import com.codahale.metrics.Timer;

public class MetricUtils {

	private MetricUtils() {
	}

	public static boolean isFasterThanXPercentOfAllRequests(long executionTimeNanos, double percentileLimit, Timer timer) {
		boolean faster = true;
		if (percentileLimit > 0) {
			if (percentileLimit >= 1) {
				faster = false;
			} else {
				final double percentile = timer.getSnapshot().getValue(percentileLimit);
				if (executionTimeNanos < percentile) {
					faster = false;
				}
			}
		}
		return faster;
	}

}
