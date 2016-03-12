package org.stagemonitor.core.metrics;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.metrics.metrics2.ScheduledMetrics2Reporter;

/**
 * The MetricsAggregationReporter computes aggregated values of all the metrics and hands them over to a list of other
 * reporters on server shutdown.
 */
public class MetricsAggregationReporter extends ScheduledMetrics2Reporter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final List<ScheduledMetrics2Reporter> onShutdownReporters;
	private Map<MetricName, Gauge> aggregatedGauges = new HashMap<MetricName, Gauge>();
	private Map<MetricName, Counter> counters = new HashMap<MetricName, Counter>();
	private Map<MetricName, Histogram> aggregatedHistograms = new HashMap<MetricName, Histogram>();
	private Map<MetricName, Meter> meters = new HashMap<MetricName, Meter>();
	private Map<MetricName, Timer> aggregatedTimers = new HashMap<MetricName, Timer>();

	public static Builder forRegistry(Metric2Registry registry) {
		return new Builder(registry);
	}

	public MetricsAggregationReporter(Builder builder) {
		super(builder);
		this.onShutdownReporters = builder.getOnShutdownReporters();
	}

	@Override
	public void reportMetrics(Map<MetricName, Gauge> gauges, Map<MetricName, Counter> counters, Map<MetricName, Histogram> histograms,
							  Map<MetricName, Meter> meters, Map<MetricName, Timer> timers) {

		for (Map.Entry<MetricName, Gauge> entry : gauges.entrySet()) {
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
		for (Map.Entry<MetricName, Histogram> entry : histograms.entrySet()) {
			if (aggregatedHistograms.containsKey(entry.getKey())) {
				((AggregatedHistogram) aggregatedHistograms.get(entry.getKey())).add(entry.getValue());
			} else {
				aggregatedHistograms.put(entry.getKey(), new AggregatedHistogram(entry.getValue()));
			}
		}
		this.meters = meters;
		for (Map.Entry<MetricName, Timer> entry : timers.entrySet()) {
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
		for (ScheduledMetrics2Reporter onShutdownReporter : onShutdownReporters) {
			try {
				onShutdownReporter.reportMetrics(aggregatedGauges, counters, aggregatedHistograms, meters, aggregatedTimers);
			} catch (RuntimeException e) {
				logger.warn(e.getMessage() + " (this exception was ignored)", e);
			}
		}
	}

	/**
	 * Computes the average without storing all measurements
	 *
	 * @param average  the current average (initially 0)
	 * @param count    the number of measurements (initially 0)
	 * @param newValue the value of the current measurement
	 * @return the arithmetic mean
	 */
	public static double computeMovingAverage(double average, int count, double newValue) {
		return (average * count + newValue) / (count + 1);
	}

	public static class Builder extends ScheduledMetrics2Reporter.Builder<MetricsAggregationReporter, Builder> {
		private final List<ScheduledMetrics2Reporter> onShutdownReporters = new LinkedList<ScheduledMetrics2Reporter>();

		/**
		 * @param registry the registry to report
		 */
		public Builder(Metric2Registry registry) {
			super(registry, "stagemonitor-aggregation-reporter");
		}

		public List<ScheduledMetrics2Reporter> getOnShutdownReporters() {
			return onShutdownReporters;
		}

		public Builder addOnShutdownReporter(ScheduledMetrics2Reporter reporter) {
			onShutdownReporters.add(reporter);
			return this;
		}

		@Override
		public MetricsAggregationReporter build() {
			return new MetricsAggregationReporter(this);
		}

		public Builder onShutdownReporters(List<ScheduledMetrics2Reporter> onShutdownReporters) {
			this.onShutdownReporters.addAll(onShutdownReporters);
			return this;
		}
	}
}
