package org.stagemonitor.core.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MetricsAggregationReporter computes aggregated values of all the metrics and hands them over to a list of other
 * reporters on server shutdown.
 */
public class MetricsAggregationReporter extends ScheduledReporter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final List<ScheduledReporter> onShutdownReporters;
	private SortedMap<String, Gauge> aggregatedGauges = new TreeMap<String, Gauge>();
	private SortedMap<String, Counter> counters = new TreeMap<String, Counter>();
	private SortedMap<String, Histogram> aggregatedHistograms = new TreeMap<String, Histogram>();
	private SortedMap<String, Meter> meters = new TreeMap<String, Meter>();
	private SortedMap<String, Timer> aggregatedTimers = new TreeMap<String, Timer>();

	/**
	 *
	 * @param registry the registry to report
	 * @param filter a {@link com.codahale.metrics.MetricFilter}
	 * @param onShutdownReporters a list of reporters that are invoked on server shutdown to report the aggregated metrics
	 */
	public MetricsAggregationReporter(MetricRegistry registry, MetricFilter filter, List<ScheduledReporter> onShutdownReporters) {
		super(registry, "aggregation-reporter", filter, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
		this.onShutdownReporters = Collections.unmodifiableList(new ArrayList<ScheduledReporter>(onShutdownReporters));
	}

	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms,
					   SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {

		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			if (entry.getValue().getValue() instanceof Number) {
				final Gauge<Number> gauge = (Gauge<Number>) entry.getValue();
				if (aggregatedGauges.containsKey(entry.getKey())) {
					((AggregatedGauge) aggregatedGauges.get(entry.getKey())).add(gauge);
				} else {
					aggregatedGauges.put(entry.getKey(), new AggregatedGauge(gauge));
				}
			} else {
				aggregatedGauges.put(entry.getKey(), entry.getValue());
			}
		}
		this.counters = counters;
		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			if (aggregatedHistograms.containsKey(entry.getKey())) {
				((AggregatedHistogram) aggregatedHistograms.get(entry.getKey())).add(entry.getValue());
			} else {
				aggregatedHistograms.put(entry.getKey(), new AggregatedHistogram(entry.getValue()));
			}
		}
		this.meters = meters;
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			if (aggregatedTimers.containsKey(entry.getKey())) {
				((AggregatedTimer) aggregatedTimers.get(entry.getKey())).add(entry.getValue());
			} else {
				aggregatedTimers.put(entry.getKey(), new AggregatedTimer(entry.getValue()));
			}
		}
	}

	/**
	 * Should be called just before the server is shutting down.
	 * The aggregated metrics are then reported by the {@link #onShutdownReporters}
	 */
	public void onShutDown() {
		for (ScheduledReporter onShutdownReporter : onShutdownReporters) {
			try {
				onShutdownReporter.report(aggregatedGauges, counters, aggregatedHistograms, meters, aggregatedTimers);
			} catch (RuntimeException e) {
				logger.warn(e.getMessage() + " (this exception was ignored)", e);
			}
		}
	}

	/**
	 * Computes the average without storing all measurements
	 *
	 * @param average the current average (initially 0)
	 * @param count the number of measurements (initially 0)
	 * @param newValue the value of the current measurement
	 * @return the arithmetic mean
	 */
	public static double computeMovingAverage(double average, int count, double newValue) {
		return (average * count + newValue) / (count + 1);
	}

}
