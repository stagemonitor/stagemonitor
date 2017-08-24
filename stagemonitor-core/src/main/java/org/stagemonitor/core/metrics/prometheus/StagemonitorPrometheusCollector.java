package org.stagemonitor.core.metrics.prometheus;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import io.prometheus.client.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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
		MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample(name.getName(), name.getTagKeys(), name.getTagValues(),
				(double) counter.getCount());
		return new MetricFamilySamples(name.getName(), Type.GAUGE, getHelpMessage(name, counter), Collections.singletonList(sample));
	}

	private static String getHelpMessage(MetricName metricName, Metric metric) {
		return String.format("Generated from stagemonitor metric import (metric=%s, type=%s)",
				metricName.getName(), metric.getClass().getName());
	}

	/**
	 * Export gauge as a prometheus gauge.
	 */
	MetricFamilySamples fromGauge(MetricName name, Gauge gauge) {
		Object obj = gauge.getValue();
		double value;
		if (obj instanceof Number) {
			value = ((Number) obj).doubleValue();
		} else if (obj instanceof Boolean) {
			value = ((Boolean) obj) ? 1 : 0;
		} else {
			logger.debug("Invalid type for Gauge {}: {}", name, obj.getClass().getName());
			return null;
		}
		MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample(name.getName(), name.getTagKeys(), name.getTagValues(), value);
		return new MetricFamilySamples(name.getName(), Type.GAUGE, getHelpMessage(name, gauge), Collections.singletonList(sample));
	}

	/**
	 * Export a histogram snapshot as a prometheus SUMMARY.
	 *  @param factor   a factor to apply to histogram values.
	 * @param name     metric name.
	 * @param nameExtra
	 * @param snapshot the histogram snapshot.
	 * @param count    the total sample count for this snapshot.
	 * @param conversionFactor
	 */
	MetricFamilySamples fromSnapshotAndCount(MetricName name, final String nameExtra, Snapshot snapshot, long count, String helpMessage, final double conversionFactor) {
		List<String> labelKeys = addToList(name.getTagKeys(), "quantile");
		final String fullName = name.getName() + nameExtra;
		List<MetricFamilySamples.Sample> samples = Arrays.asList(
				new MetricFamilySamples.Sample(fullName, labelKeys, addToList(name.getTagValues(), "0.5"), snapshot.getMedian() / conversionFactor),
				new MetricFamilySamples.Sample(fullName, labelKeys, addToList(name.getTagValues(), "0.75"), snapshot.get75thPercentile() / conversionFactor),
				new MetricFamilySamples.Sample(fullName, labelKeys, addToList(name.getTagValues(), "0.95"), snapshot.get95thPercentile() / conversionFactor),
				new MetricFamilySamples.Sample(fullName, labelKeys, addToList(name.getTagValues(), "0.98"), snapshot.get98thPercentile() / conversionFactor),
				new MetricFamilySamples.Sample(fullName, labelKeys, addToList(name.getTagValues(), "0.99"), snapshot.get99thPercentile() / conversionFactor),
				new MetricFamilySamples.Sample(fullName, labelKeys, addToList(name.getTagValues(), "0.999"), snapshot.get999thPercentile() / conversionFactor),
				new MetricFamilySamples.Sample(fullName + "_count", name.getTagKeys(), name.getTagValues(), count)
		);
		return new MetricFamilySamples(fullName, Type.SUMMARY, helpMessage, samples);
	}

	private List<String> addToList(List<String> list, String additional) {
		final List<String> newList = new ArrayList<String>(list);
		newList.add(additional);
		return newList;
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
		final Map<String,List<MetricFamilySamples>> collectedMfSamples = new TreeMap<String, List<MetricFamilySamples>>();

		for (SortedMap.Entry<MetricName, Gauge> entry : registry.getGauges().entrySet()) {
			final MetricFamilySamples metricFamilySamples = fromGauge(entry.getKey(), entry.getValue());
			if (metricFamilySamples != null) {
				putSamples(collectedMfSamples, metricFamilySamples);
			}
		}
		for (SortedMap.Entry<MetricName, Counter> entry : registry.getCounters().entrySet()) {
			putSamples(collectedMfSamples, fromCounter(entry.getKey(), entry.getValue()));
		}
		for (SortedMap.Entry<MetricName, Histogram> entry : registry.getHistograms().entrySet()) {
			putSamples(collectedMfSamples, fromHistogram(entry.getKey(), entry.getValue()));
		}
		for (SortedMap.Entry<MetricName, Timer> entry : registry.getTimers().entrySet()) {
			putSamples(collectedMfSamples, fromTimer(entry.getKey(), entry.getValue()));
		}
		for (SortedMap.Entry<MetricName, Meter> entry : registry.getMeters().entrySet()) {
			putSamples(collectedMfSamples, fromMeter(entry.getKey(), entry.getValue()));
		}

		final List<MetricFamilySamples> uniqueMfSamples = new ArrayList<MetricFamilySamples>(registry.getMetrics().size());
		for (Map.Entry<String,List<MetricFamilySamples>> mfSamples : collectedMfSamples.entrySet()) {
			uniqueMfSamples.add(combineMfSamples(mfSamples.getKey(), mfSamples.getValue()));
		}

		return uniqueMfSamples;
	}

	private MetricFamilySamples combineMfSamples(final String name, final List<MetricFamilySamples> samplesList) {
		if (samplesList.isEmpty()) {
			throw new IllegalArgumentException("sampleList is expected to have at list one MetricFamilySamples");
		}

		final Set<Collector.Type> types = new HashSet<Type>();
		final List<MetricFamilySamples.Sample> combinedSamples = new ArrayList<MetricFamilySamples.Sample>();

		for (final MetricFamilySamples samples : samplesList) {
			types.add(samples.type);
			combinedSamples.addAll(samples.samples);
		}

		if (types.size() > 1) {
			throw new IllegalStateException("Metric with name " + name + " comes with different types, that is not supported. " + types);
		}

		final MetricFamilySamples first = samplesList.get(0);
		return new MetricFamilySamples(name, first.type, first.help, combinedSamples);
	}

	private void putSamples(final Map<String, List<MetricFamilySamples>> collectedMfSamples,
							final MetricFamilySamples metricFamilySamples) {
		if (!collectedMfSamples.containsKey(metricFamilySamples.name)) {
			collectedMfSamples.put(metricFamilySamples.name, new LinkedList<MetricFamilySamples>());
		}

		collectedMfSamples.get(metricFamilySamples.name).add(metricFamilySamples);
	}
}
