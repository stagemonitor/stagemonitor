package org.stagemonitor.core.metrics;

import com.codahale.metrics.Gauge;

import static org.stagemonitor.core.metrics.MetricsAggregationReporter.computeMovingAverage;

/**
 * Computes the average of multiple snapshots of a {@link Gauge}<? extends {@link Number}>
 */
public class AggregatedGauge implements Gauge<Double> {

	private double aggregatedNumberValue;
	private int count;

	public AggregatedGauge(Gauge<? extends Number> gauge) {
		add(gauge);
	}

	@Override
	public Double getValue() {
		return aggregatedNumberValue;
	}

	public void add(Gauge<? extends Number> gauge) {
		aggregatedNumberValue = computeMovingAverage(aggregatedNumberValue, count, gauge.getValue().doubleValue());
		count++;
	}
}
