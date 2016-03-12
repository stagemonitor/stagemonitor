package org.stagemonitor.core.metrics.metrics2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.ExecutorUtils;

/**
 * A {@link ScheduledReporter} that works with a {@link Metric2Registry}
 */
public abstract class ScheduledMetrics2Reporter extends ScheduledReporter {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledMetrics2Reporter.class);

	protected final Metric2Registry registry;
	private final Metric2Filter filter;
	private final ScheduledExecutorService executor;
	protected Clock clock;
	private boolean started;

	protected ScheduledMetrics2Reporter(Builder builder) {
		super(null, null, null, builder.getRateUnit(), builder.getDurationUnit(), builder.getExecutor());
		this.registry = builder.getRegistry();
		this.filter = builder.getFilter();
		this.executor = builder.getExecutor();
		this.clock = builder.getClock();
	}

	@Override
	public void report() {
		reportMetrics(
				registry.getGauges(filter),
				registry.getCounters(filter),
				registry.getHistograms(filter),
				registry.getMeters(filter),
				registry.getTimers(filter)
		);
	}

	/**
	 * Called periodically by the polling thread. Subclasses should report all the given metrics.
	 *
	 * @param gauges     all of the gauges in the registry
	 * @param counters   all of the counters in the registry
	 * @param histograms all of the histograms in the registry
	 * @param meters     all of the meters in the registry
	 * @param timers     all of the timers in the registry
	 */
	public abstract void reportMetrics(Map<MetricName, Gauge> gauges,
									   Map<MetricName, Counter> counters,
									   Map<MetricName, Histogram> histograms,
									   Map<MetricName, Meter> meters,
									   Map<MetricName, Timer> timers);

	/**
	 * Don't use this method
	 *
	 * @deprecated use {@link #reportMetrics(Map, Map, Map, Map, Map)}
	 */
	@Override
	@Deprecated
	public final void report(SortedMap<String, Gauge> gauges,
							 SortedMap<String, Counter> counters,
							 SortedMap<String, Histogram> histograms,
							 SortedMap<String, Meter> meters,
							 SortedMap<String, Timer> timers) {
		// intentionally left blank
	}


	/**
	 * Starts the reporter polling at the given period.
	 *
	 * @param period the amount of time between polls
	 * @param unit   the unit for {@code period}
	 */
	public void start(long period, TimeUnit unit) {
		synchronized (this) {
			if (started) {
				throw new IllegalStateException("This reporter has already been started");
			}
			final long periodInMS = unit.toMillis(period);
			this.clock = new QuantizedClock(clock, periodInMS);
			executor.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						report();
					} catch (RuntimeException ex) {
						logger.error("RuntimeException thrown from {}#report. Exception was suppressed.", getClass().getSimpleName(), ex);
					}
				}
			}, getNextTimestampThatIsDivisableByPeriod(System.currentTimeMillis(), periodInMS), periodInMS, TimeUnit.MILLISECONDS);
			this.started = true;
		}
	}

	/*
	 * Makes sure that values are always submitted at the same time on each server no matter when they where started (aka. quantization)
	 *
	 * See https://blog.raintank.io/how-to-effectively-use-the-elasticsearch-data-source-and-solutions-to-common-pitfalls/#incomplete
	 * and also https://blog.raintank.io/25-graphite-grafana-and-statsd-gotchas/#graphite.quantization
	 */
	public static long getNextTimestampThatIsDivisableByPeriod(long currentTimestamp, long periodInMS) {
		final long offset = periodInMS - (currentTimestamp % periodInMS);
		return currentTimestamp + offset;
	}

	public abstract static class Builder<R extends ScheduledMetrics2Reporter, B extends Builder> {
		private final Metric2Registry registry;
		private final ScheduledExecutorService executor;
		private Metric2Filter filter = Metric2Filter.ALL;
		private TimeUnit rateUnit = TimeUnit.SECONDS;
		private TimeUnit durationUnit = TimeUnit.MILLISECONDS;
		private Clock clock = Clock.defaultClock();
		private Map<String, String> globalTags = Collections.emptyMap();

		protected Builder(Metric2Registry registry, String reporterName) {
			this.executor = Executors.newSingleThreadScheduledExecutor(new ExecutorUtils.NamedThreadFactory(reporterName));
			this.registry = registry;
		}

		protected Builder(Metric2Registry registry, ScheduledExecutorService executor) {
			this.executor = executor;
			this.registry = registry;
		}

		public Metric2Registry getRegistry() {
			return registry;
		}

		public Metric2Filter getFilter() {
			return filter;
		}

		public TimeUnit getRateUnit() {
			return rateUnit;
		}

		public TimeUnit getDurationUnit() {
			return durationUnit;
		}

		public ScheduledExecutorService getExecutor() {
			return executor;
		}

		/**
		 * Only report metrics which match the given filter.
		 *
		 * @param filter a {@link com.codahale.metrics.MetricFilter}
		 * @return {@code this}
		 */
		public B filter(Metric2Filter filter) {
			this.filter = filter;
			return (B) this;
		}

		/**
		 * Convert rates to the given time unit.
		 *
		 * @param rateUnit a unit of time
		 * @return {@code this}
		 */
		public B convertRatesTo(TimeUnit rateUnit) {
			this.rateUnit = rateUnit;
			return (B) this;
		}


		/**
		 * Convert durations to the given time unit.
		 *
		 * @param durationUnit a unit of time
		 * @return {@code this}
		 */
		public B convertDurationsTo(TimeUnit durationUnit) {
			this.durationUnit = durationUnit;
			return (B) this;
		}

		public Clock getClock() {
			return clock;
		}

		public B clock(Clock clock) {
			this.clock = clock;
			return (B) this;
		}

		public Map<String, String> getGlobalTags() {
			return globalTags;
		}

		public B globalTags(Map<String, String> globalTags) {
			this.globalTags = Collections.unmodifiableMap(new LinkedHashMap<String, String>(globalTags));
			return (B) this;
		}

		/**
		 * Builds a reporter with the given properties.
		 *
		 * @return a reporter
		 */
		public abstract R build();
	}
}
