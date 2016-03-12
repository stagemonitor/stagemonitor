package org.stagemonitor.core.metrics.metrics2;

import com.codahale.metrics.Clock;

/**
 * The quantized clock always returns a value for {@link #getTime()} that is divisable by {@link #periodInMS}
 * <p/>
 * This makes all reporters report at the exact same instant. Even if their clocks are slightly different,
 * their reported timestamp is the same.
 */
public class QuantizedClock extends Clock {
	private final Clock delegate;
	private final long periodInMS;

	public QuantizedClock(Clock delegate, long periodInMS) {
		this.delegate = delegate;
		this.periodInMS = periodInMS;
	}

	@Override
	public long getTick() {
		return delegate.getTick();
	}

	@Override
	public long getTime() {
		final long time = delegate.getTime();
		return time - (time % periodInMS);
	}
}
