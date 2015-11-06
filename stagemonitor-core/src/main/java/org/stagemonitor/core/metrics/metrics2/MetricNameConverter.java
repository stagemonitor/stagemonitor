package org.stagemonitor.core.metrics.metrics2;

/**
 * Converts a old.fashioned.metric.name to a metric 2.0 style {@link MetricName}
 */
public interface MetricNameConverter {
	MetricName convert(String name);
}
