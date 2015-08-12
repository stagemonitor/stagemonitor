package org.stagemonitor.core.metrics.metrics2;

import java.util.Map;

import com.codahale.metrics.Metric;

/**
 * A set of named metrics.
 *
 * @see Metric2Registry#registerAll(Metric2Set)
 */
public interface Metric2Set {

	/**
	 * A map of metric names to metrics.
	 *
	 * @return the metrics
	 */
	Map<MetricName, Metric> getMetrics();
}
