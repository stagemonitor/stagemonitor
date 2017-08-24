package org.stagemonitor.core.metrics.prometheus;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private CounterMetricFamily fromCounter(List<Map.Entry<MetricName, Counter>> countersWithSameName) {
		final Map.Entry<MetricName, Counter> first = countersWithSameName.get(0);
		final MetricName firstName = first.getKey();
		final CounterMetricFamily metricFamily = new CounterMetricFamily(firstName.getName(), getHelpMessage(firstName, first.getValue()), firstName.getTagKeys());
		for (Map.Entry<MetricName, Counter> entry : countersWithSameName) {
			metricFamily.addMetric(entry.getKey().getTagValues(), entry.getValue().getCount());
		}
		return metricFamily;
	}

	private static String getHelpMessage(MetricName metricName, Metric metric) {
		return String.format("Generated from stagemonitor metric import (metric=%s, type=%s)",
				metricName.getName(), metric.getClass().getName());
	}

	/**
	 * Export gauge as a prometheus gauge.
	 */
	private GaugeMetricFamily fromGauge(List<Map.Entry<MetricName, Gauge>> gaugesWithSameName) {
		final Map.Entry<MetricName, Gauge> first = gaugesWithSameName.get(0);
		final MetricName firstName = first.getKey();
		final GaugeMetricFamily gaugeMetricFamily = new GaugeMetricFamily(firstName.getName(), getHelpMessage(first.getKey(), first.getValue()), first.getKey().getTagKeys());
		for (Map.Entry<MetricName, Gauge> entry : gaugesWithSameName) {
			gaugeMetricFamily.addMetric(entry.getKey().getTagValues(), getDoubleFromGauge(entry.getValue()));
		}
		return gaugeMetricFamily;
	}

	private Double getDoubleFromGauge(Gauge gauge) {
		if (gauge.getValue() instanceof Number) {
			return ((Number) gauge.getValue()).doubleValue();
		} else if (gauge.getValue() instanceof Boolean) {
			return ((Boolean) gauge.getValue()) ? 1.0 : 0;
		} else {
			return -1d;
		}
	}

	/**
	 * Convert histogram snapshot.
	 */
	private SummaryMetricFamily fromHistogram(List<Map.Entry<MetricName, Histogram>> histogramsWithSameName) {
		final SummaryMetricFamily summaryMetricFamily = getSummaryMetricFamily(histogramsWithSameName, "");
		for (Map.Entry<MetricName, Histogram> entry : histogramsWithSameName) {
			addSummaryMetric(summaryMetricFamily, entry.getKey(), entry.getValue().getSnapshot(), 1.0D, entry.getValue().getCount());
		}
		return summaryMetricFamily;
	}

	/**
	 * Export dropwizard Timer as a histogram. Use TIME_UNIT as time unit.
	 */
	private MetricFamilySamples fromTimer(List<Map.Entry<MetricName, Timer>> histogramsWithSameName) {
		final SummaryMetricFamily summaryMetricFamily = getSummaryMetricFamily(histogramsWithSameName, "_seconds");
		for (Map.Entry<MetricName, Timer> entry : histogramsWithSameName) {
			addSummaryMetric(summaryMetricFamily, entry.getKey(), entry.getValue().getSnapshot(), SECONDS_IN_NANOS, entry.getValue().getCount());
		}
		return summaryMetricFamily;
	}

	private void addSummaryMetric(SummaryMetricFamily summaryMetricFamily, MetricName name, Snapshot snapshot, double conversionFactor, long count) {
		summaryMetricFamily.addMetric(name.getTagValues(), count, -1, Arrays.asList(snapshot.getMedian() / conversionFactor,
				snapshot.get75thPercentile() / conversionFactor,
				snapshot.get95thPercentile() / conversionFactor,
				snapshot.get98thPercentile() / conversionFactor,
				snapshot.get99thPercentile() / conversionFactor,
				snapshot.get999thPercentile() / conversionFactor));
	}

	private <T extends Metric> SummaryMetricFamily getSummaryMetricFamily(List<Map.Entry<MetricName, T>> summariesWithSameName, String nameSuffix) {
		final Map.Entry<MetricName, T> first = summariesWithSameName.get(0);
		final MetricName firstName = first.getKey();
		final String fullName = firstName.getName() + nameSuffix;
		return new SummaryMetricFamily(fullName, getHelpMessage(first.getKey(), first.getValue()), firstName.getTagKeys(), Arrays.asList(0.5, 0.75, 0.95, 0.98, 0.99, 0.999));
	}

	private <M extends Metered> MetricFamilySamples fromMeter(List<Map.Entry<MetricName, M>> metersWithSameName, String suffix) {
		final Map.Entry<MetricName, M> first = metersWithSameName.get(0);
		final MetricName firstName = first.getKey();
		ArrayList<MetricFamilySamples.Sample> sampleList = new ArrayList<MetricFamilySamples.Sample>(metersWithSameName.size());
		final String name = firstName.getName() + suffix;
		for (Map.Entry<MetricName, M> entry : metersWithSameName) {
			final M metered = entry.getValue();
			sampleList.add(new MetricFamilySamples.Sample(name + "_total", entry.getKey().getTagKeys(), entry.getKey().getTagValues(), metered.getCount()));
			sampleList.add(new MetricFamilySamples.Sample(name + "_m1", entry.getKey().getTagKeys(), entry.getKey().getTagValues(), metered.getOneMinuteRate()));
			sampleList.add(new MetricFamilySamples.Sample(name + "_m5", entry.getKey().getTagKeys(), entry.getKey().getTagValues(), metered.getFiveMinuteRate()));
			sampleList.add(new MetricFamilySamples.Sample(name + "_m15", entry.getKey().getTagKeys(), entry.getKey().getTagValues(), metered.getFifteenMinuteRate()));
		}
		return new MetricFamilySamples(name, Type.UNTYPED, getHelpMessage(firstName, first.getValue()), sampleList);
	}

	@Override
	public List<MetricFamilySamples> collect() {
		groupByName(registry.getMetrics());
		ArrayList<MetricFamilySamples> mfSamples = new ArrayList<MetricFamilySamples>(registry.getMetrics().size());
		for (List<Map.Entry<MetricName, Gauge>> entry : groupByName(registry.getGauges()).values()) {
			mfSamples.add(fromGauge(entry));
		}
		for (List<Map.Entry<MetricName, Counter>> entry : groupByName(registry.getCounters()).values()) {
			mfSamples.add(fromCounter(entry));
		}
		for (List<Map.Entry<MetricName, Histogram>> entry : groupByName(registry.getHistograms()).values()) {
			mfSamples.add(fromHistogram(entry));
		}
		for (List<Map.Entry<MetricName, Timer>> entry : groupByName(registry.getTimers()).values()) {
			mfSamples.add(fromTimer(entry));
			mfSamples.add(fromMeter(entry, "_meter"));
		}
		for (List<Map.Entry<MetricName, Meter>> entry : groupByName(registry.getMeters()).values()) {
			mfSamples.add(fromMeter(entry, ""));
		}
		return mfSamples;
	}

	private <T extends Metric> Map<String, List<Map.Entry<MetricName, T>>> groupByName(Map<MetricName, T> metrics) {
		Map<String, List<Map.Entry<MetricName, T>>> metricsByName = new HashMap<String, List<Map.Entry<MetricName, T>>>();
		for (Map.Entry<MetricName, T> entry : metrics.entrySet()) {
			if (metricsByName.containsKey(entry.getKey().getName())) {
				metricsByName.get(entry.getKey().getName()).add(entry);
			} else {
				final ArrayList<Map.Entry<MetricName, T>> values = new ArrayList<Map.Entry<MetricName, T>>();
				values.add(entry);
				metricsByName.put(entry.getKey().getName(), values);
			}
		}
		return metricsByName;
	}
}
