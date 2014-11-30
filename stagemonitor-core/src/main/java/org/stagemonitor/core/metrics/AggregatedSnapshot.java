package org.stagemonitor.core.metrics;

import com.codahale.metrics.Snapshot;

import java.io.OutputStream;

class AggregatedSnapshot extends Snapshot {
	private int addCount = 0;
	private long max, min;
	private double mean, stdDev, median, p75, p95, p98, p99, p999;

	AggregatedSnapshot(Snapshot snapshot) {
		this.max = snapshot.getMax();
		this.min = snapshot.getMin();
	}

	public void add(Snapshot snapshot) {
		max = Math.max(max, snapshot.getMax());
		min = Math.min(min, snapshot.getMin());
		mean = MetricsAggregationReporter.computeMovingAverage(mean, addCount, snapshot.getMean());
		stdDev = MetricsAggregationReporter.computeMovingAverage(stdDev, addCount, snapshot.getStdDev());
		median = MetricsAggregationReporter.computeMovingAverage(median, addCount, snapshot.getMedian());
		p75 = MetricsAggregationReporter.computeMovingAverage(p75, addCount, snapshot.get75thPercentile());
		p95 = MetricsAggregationReporter.computeMovingAverage(p95, addCount, snapshot.get95thPercentile());
		p98 = MetricsAggregationReporter.computeMovingAverage(p98, addCount, snapshot.get98thPercentile());
		p99 = MetricsAggregationReporter.computeMovingAverage(p99, addCount, snapshot.get99thPercentile());
		p999 = MetricsAggregationReporter.computeMovingAverage(p999, addCount, snapshot.get999thPercentile());
		addCount++;
	}

	@Override
	public double getValue(double quantile) {
		return 0;
	}

	@Override
	public long[] getValues() {
		return new long[0];
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public long getMax() {
		return max;
	}

	@Override
	public double getMean() {
		return mean;
	}

	@Override
	public long getMin() {
		return min;
	}

	@Override
	public double getStdDev() {
		return stdDev;
	}

	@Override
	public double getMedian() {
		return median;
	}

	@Override
	public double get75thPercentile() {
		return p75;
	}

	@Override
	public double get95thPercentile() {
		return p95;
	}

	@Override
	public double get98thPercentile() {
		return p98;
	}

	@Override
	public double get99thPercentile() {
		return p99;
	}

	@Override
	public double get999thPercentile() {
		return p999;
	}

	@Override
	public void dump(OutputStream output) {
	}
}
