package org.stagemonitor.core.metrics;

import com.codahale.metrics.Meter;
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

	public static boolean isRateLimitExceeded(double maxRate, Meter meter) {
		return maxRate < 1000000 && (maxRate <= 0 || 60 * meter.getOneMinuteRate() > maxRate);
	}
}
