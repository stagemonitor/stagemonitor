package org.stagemonitor.core.metrics.metrics2;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

	protected ScheduledMetrics2Reporter(Metric2Registry registry, Metric2Filter filter, TimeUnit rateUnit, TimeUnit durationUnit, String name) {
		this(registry, filter, rateUnit, durationUnit, Executors.newSingleThreadScheduledExecutor(new ExecutorUtils.NamedThreadFactory(name)));
	}

	protected ScheduledMetrics2Reporter(Metric2Registry registry, Metric2Filter filter, TimeUnit rateUnit, TimeUnit durationUnit, ScheduledExecutorService executor) {
		super(null, null, null, rateUnit, durationUnit, executor);
		this.registry = registry;
		this.filter = filter;
		this.executor = executor;
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
		final long periodInMS = unit.toMillis(period);
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

	public static void main(String[] args) {
		final long currentTimeMillis = 1457530360001L; //System.currentTimeMillis();
		final long nextTimestampThatIsDivisableByPeriod = getNextTimestampThatIsDivisableByPeriod(currentTimeMillis, 10000);
		System.out.println(currentTimeMillis);
		System.out.println(nextTimestampThatIsDivisableByPeriod);
	}

}
