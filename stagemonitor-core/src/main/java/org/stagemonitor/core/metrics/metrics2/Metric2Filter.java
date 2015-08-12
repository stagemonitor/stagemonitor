package org.stagemonitor.core.metrics.metrics2;

import com.codahale.metrics.Metric;

/**
 * A filter used to determine whether or not a metric should be reported, among other things.
 */
public interface Metric2Filter {
	/**
	 * Matches all metrics, regardless of type or name.
	 */
	Metric2Filter ALL = new Metric2Filter() {
		@Override
		public boolean matches(MetricName name, Metric metric) {
			return true;
		}
	};

	/**
	 * Returns {@code true} if the metric matches the filter; {@code false} otherwise.
	 *
	 * @param name      the metric's name
	 * @param metric    the metric
	 * @return {@code true} if the metric matches the filter
	 */
	boolean matches(MetricName name, Metric metric);


}
