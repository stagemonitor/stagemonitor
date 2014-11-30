package org.stagemonitor.core.metrics;

import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.concurrent.TimeUnit;

/**
 * An extension of timer that aggregates multiple snapshots of a timer to a single value for each metric.
 */
class AggregatedTimer extends Timer {
	private long count;
	private double rate;
	private AggregatedSnapshot snapshot;

	AggregatedTimer(Timer timer) {
		super(new SlidingWindowReservoir(0));
		this.snapshot = new AggregatedSnapshot(timer.getSnapshot());
		add(timer);
	}

	public void add(Timer timer) {
		count = timer.getCount();
		rate = timer.getMeanRate();
		snapshot.add(timer.getSnapshot());
	}

	@Override
	public void update(long duration, TimeUnit unit) {
	}

	@Override
	public long getCount() {
		return count;
	}

	@Override
	public double getFifteenMinuteRate() {
		return rate;
	}

	@Override
	public double getFiveMinuteRate() {
		return rate;
	}

	@Override
	public double getMeanRate() {
		return rate;
	}

	@Override
	public double getOneMinuteRate() {
		return rate;
	}

	@Override
	public Snapshot getSnapshot() {
		return snapshot;
	}

}
