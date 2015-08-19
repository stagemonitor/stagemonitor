package org.stagemonitor.core.metrics.metrics2;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;

public abstract class ScheduledMetrics2Reporter extends ScheduledReporter {

	protected final Metric2Registry registry;
	private final Metric2Filter filter;

	protected ScheduledMetrics2Reporter(Metric2Registry registry, Metric2Filter filter, TimeUnit rateUnit, TimeUnit durationUnit) {
		super(null, null, null, rateUnit, durationUnit);
		this.registry = registry;
		this.filter = filter;
	}

	protected ScheduledMetrics2Reporter(Metric2Registry registry, Metric2Filter filter, TimeUnit rateUnit, TimeUnit durationUnit, ScheduledExecutorService executor) {
		super(null, null, null, rateUnit, durationUnit, executor);
		this.registry = registry;
		this.filter = filter;
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

}
