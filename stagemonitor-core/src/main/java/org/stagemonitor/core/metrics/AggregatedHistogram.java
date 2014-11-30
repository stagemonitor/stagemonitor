package org.stagemonitor.core.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Snapshot;

/**
 * An extension of histogram that aggregates multiple snapshots of a histogram to a single value for each metric.
 */
class AggregatedHistogram extends Histogram {

	private long count;
	private AggregatedSnapshot snapshot;

	public AggregatedHistogram(Histogram histogram) {
		super(new SlidingWindowReservoir(0));
		snapshot = new AggregatedSnapshot(histogram.getSnapshot());
		add(histogram);
	}

	@Override
	public void update(int value) {
	}

	@Override
	public void update(long value) {
	}

	@Override
	public long getCount() {
		return count;
	}

	@Override
	public Snapshot getSnapshot() {
		return snapshot;
	}

	public void add(Histogram histogram) {
		this.count = histogram.getCount();
		snapshot.add(histogram.getSnapshot());
	}
}
