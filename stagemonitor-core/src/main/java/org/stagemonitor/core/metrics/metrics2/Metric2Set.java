package org.stagemonitor.core.metrics.metrics2;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	class Converter {
		private final static Logger logger = LoggerFactory.getLogger(Converter.class);

		public static Metric2Set convert(final MetricSet metricSet, final Map<String, MetricName> metricNameMapping) {
			return new Metric2Set() {
				@Override
				public Map<MetricName, Metric> getMetrics() {
					final HashMap<MetricName, Metric> result = new HashMap<MetricName, Metric>();
					for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
						if (metricNameMapping.containsKey(entry.getKey())) {
							result.put(metricNameMapping.get(entry.getKey()), entry.getValue());
						} else {
							logger.warn("No mapping for key " + entry.getKey());
						}
					}
					return result;
				}
			};
		}
		public static Metric2Set convert(final MetricSet metricSet, final MetricNameConverter converter) {
			return new Metric2Set() {
				@Override
				public Map<MetricName, Metric> getMetrics() {
					final HashMap<MetricName, Metric> result = new HashMap<MetricName, Metric>();
					for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
						final MetricName convertedName;
						try {
							convertedName = converter.convert(entry.getKey());
							result.put(convertedName, entry.getValue());
						} catch (Exception e) {
							e.printStackTrace();
							logger.warn("Invalid name " + entry.getKey());
							logger.debug(e.getMessage(), e);
						}
					}
					return result;
				}
			};
		}
	}
}
