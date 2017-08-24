package org.stagemonitor.core.metrics.prometheus;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.SummaryMetricFamily;

/**
 * A {@link Collector} implementation for stagemonitor's {@link Metric2Registry}.
 * <p>
 * There are two ways to get metrics into Prometheus:
 * </p>
 * <p>
 * 1. Let Prometheus scrape (pull) the metrics from your server (this method is recommended by Prometheus):<br>
 * <br>
 * Add a dependency to io.prometheus:simpleclient_servlet<br>
 * Register the {@link StagemonitorPrometheusCollector} on startup of your application:
 * <pre>
 *     CollectorRegistry.defaultRegistry.register(new StagemonitorPrometheusCollector(Stagemonitor.getMetric2Registry()));
 * </pre>
 * Add the io.prometheus.client.exporter.MetricsServlet to your web.xml, and configure your
 * <a href="https://prometheus.io/docs/operating/configuration/#%3Cscrape_config%3E">prometheus scrape</a> accordingly.
 * <p/>
 *
 * <p>
 * 2. Push metrics to Prometheus push gateway:<br>
 *
 * Add a dependency to io.prometheus:simpleclient_pushgateway<br>
 * <pre>
 *     PushGateway pg = new PushGateway("127.0.0.1:9091");
 *     CollectorRegistry registry = new CollectorRegistry();
 *     collectorRegistry.register(new StagemonitorPrometheusCollector(Stagemonitor.getMetric2Registry()));
 *     pg.pushAdd(registry, "my_batch_job");
 * </pre>
 * See also https://prometheus.io/docs/instrumenting/pushing/
 * </p>
 */
public class StagemonitorPrometheusCollector extends Collector {

	private static final double SECONDS_IN_NANOS = TimeUnit.SECONDS.toNanos(1);

	private static final Logger logger = LoggerFactory.getLogger(StagemonitorPrometheusCollector.class);
	private Metric2Registry registry;

	/**
	 * @param registry a metric registry to export in prometheus.
	 */
	public StagemonitorPrometheusCollector(Metric2Registry registry) {
		this.registry = registry;
	}

	/**
	 * Export counter as prometheus counter.
	 */
	MetricFamilySamples fromCounter(MetricName name, Counter counter) {
		final CounterMetricFamily metricFamily = new CounterMetricFamily(name.getName(), getHelpMessage(name, counter), name.getTagKeys());
		metricFamily.addMetric(name.getTagValues(), counter.getCount());
		return metricFamily;
	}

	private static String getHelpMessage(MetricName metricName, Metric metric) {
		return String.format("Generated from stagemonitor metric import (metric=%s, type=%s)",
				metricName.getName(), metric.getClass().getName());
	}

	/**
	 * Export gauge as a prometheus gauge.
	 */
	GaugeMetricFamily fromGauge(MetricName name, Gauge gauge) {
		Double value = getDoubleFromGauge(gauge);
		if (value == null) {
			logger.debug("Invalid type for Gauge {}: {}", name, gauge.getValue().getClass().getName());
			return null;
		}
		final GaugeMetricFamily gaugeMetricFamily = new GaugeMetricFamily(name.getName(), getHelpMessage(name, gauge), name.getTagKeys());
		gaugeMetricFamily.addMetric(name.getTagValues(), value);
		return gaugeMetricFamily;
	}

	private Double getDoubleFromGauge(Gauge gauge) {
		Double value;
		if (gauge.getValue() instanceof Number) {
			value = ((Number) gauge.getValue()).doubleValue();
		} else if (gauge.getValue() instanceof Boolean) {
			value = ((Boolean) gauge.getValue()) ? 1.0 : 0;
		} else {
			value = null;
		}
		return value;
	}

	/**
	 * Export a histogram snapshot as a prometheus SUMMARY.
	 *  @param conversionFactor   a factor to apply to histogram values.
	 * @param name     metric name.
	 * @param nameExtra
	 * @param snapshot the histogram snapshot.
	 * @param count    the total sample count for this snapshot.
	 * @param conversionFactor
	 */
	MetricFamilySamples fromSnapshotAndCount(MetricName name, final String nameExtra, Snapshot snapshot, long count, String helpMessage, final double conversionFactor) {
		final String fullName = name.getName() + nameExtra;
		final SummaryMetricFamily summaryMetricFamily = new SummaryMetricFamily(fullName, helpMessage, name.getTagKeys(), Arrays.asList(0.5, 0.75, 0.95, 0.98, 0.99, 0.999));

		summaryMetricFamily.addMetric(name.getTagValues(), count, -1, Arrays.asList(snapshot.getMedian() / conversionFactor,
				snapshot.get75thPercentile() / conversionFactor,
				snapshot.get95thPercentile() / conversionFactor,
				snapshot.get98thPercentile() / conversionFactor,
				snapshot.get99thPercentile() / conversionFactor,
				snapshot.get999thPercentile() / conversionFactor));
		return summaryMetricFamily;
	}

	/**
	 * Convert histogram snapshot.
	 */
	MetricFamilySamples fromHistogram(MetricName name, Histogram histogram) {
		final Snapshot snapshot = histogram.getSnapshot();
		return fromSnapshotAndCount(name, "", snapshot,
				snapshot.size(), getHelpMessage(name, histogram), 1.0D);
	}

	/**
	 * Export dropwizard Timer as a histogram. Use TIME_UNIT as time unit.
	 */
	MetricFamilySamples fromTimer(MetricName name, Timer timer) {
		final Snapshot snapshot = timer.getSnapshot();
		return fromSnapshotAndCount(name, "_seconds", snapshot,
				snapshot.size(), getHelpMessage(name, timer), SECONDS_IN_NANOS);
	}

	/**
	 * Export a Meter as as prometheus COUNTER.
	 */
	MetricFamilySamples fromMeter(MetricName name, Meter meter) {
		return new MetricFamilySamples(name.getName() + "_total", Type.COUNTER, getHelpMessage(name, meter),
				Collections.singletonList(new MetricFamilySamples.Sample(name.getName() + "_total", name.getTagKeys(),
						name.getTagValues(), meter.getCount())));
	}

	@Override
	public List<MetricFamilySamples> collect() {
		ArrayList<MetricFamilySamples> mfSamples = new ArrayList<MetricFamilySamples>(registry.getMetrics().size());
		mfSamples.addAll(getGaugeMetricFamilies());
		for (SortedMap.Entry<MetricName, Counter> entry : registry.getCounters().entrySet()) {
			mfSamples.add(fromCounter(entry.getKey(), entry.getValue()));
		}
		for (SortedMap.Entry<MetricName, Histogram> entry : registry.getHistograms().entrySet()) {
			mfSamples.add(fromHistogram(entry.getKey(), entry.getValue()));
		}
		for (SortedMap.Entry<MetricName, Timer> entry : registry.getTimers().entrySet()) {
			mfSamples.add(fromTimer(entry.getKey(), entry.getValue()));
		}
		for (SortedMap.Entry<MetricName, Meter> entry : registry.getMeters().entrySet()) {
			mfSamples.add(fromMeter(entry.getKey(), entry.getValue()));
		}
		return mfSamples;
	}

	private Collection<GaugeMetricFamily> getGaugeMetricFamilies() {
		final Map<MetricName, Gauge> gauges = registry.getGauges();
		Map<String, GaugeMetricFamily> gaugesByName = new HashMap<String, GaugeMetricFamily>(CollectionUtils.getMapCapacityForExpectedSize(gauges.size()));
		for (SortedMap.Entry<MetricName, Gauge> entry : gauges.entrySet()) {
			GaugeMetricFamily gaugeMetricFamily = gaugesByName.get(entry.getKey().getName());
			if (gaugeMetricFamily == null) {
				gaugeMetricFamily = fromGauge(entry.getKey(), entry.getValue());
			} else {
				final Double value = getDoubleFromGauge(entry.getValue());
				if (value != null) {
					gaugeMetricFamily.addMetric(entry.getKey().getTagValues(), value);
				}
			}
			gaugesByName.put(entry.getKey().getName(), gaugeMetricFamily);
		}
		return gaugesByName.values();
	}
}
