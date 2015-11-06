package org.stagemonitor.core.metrics.metrics2;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * Serves as a backwards compatibility bridge between {@link Metric2Registry} and {@link MetricRegistry}
 */
public class MetricRegistryAdapter extends MetricRegistry {

	private final Metric2Registry metric2Registry;

	public MetricRegistryAdapter(Metric2Registry metric2Registry) {
		this.metric2Registry = metric2Registry;
	}

	@Override
	public SortedSet<String> getNames() {
		SortedSet<String> result = new TreeSet<String>();
		for (MetricName metricName : metric2Registry.getNames()) {
			result.add(metricName.toGraphiteName());
		}
		result.addAll(super.getNames());
		return result;
	}

	@Override
	public SortedMap<String, Gauge> getGauges() {
		final SortedMap<String, Gauge> result = convertToDottedName(metric2Registry.getGauges());
		result.putAll(super.getGauges());
		return result;
	}

	@Override
	public SortedMap<String, Gauge> getGauges(MetricFilter filter) {
		final SortedMap<String, Gauge> result = convertToDottedName(metric2Registry.getMetrics(Gauge.class, filter));
		result.putAll(super.getGauges(filter));
		return result;
	}

	@Override
	public SortedMap<String, Counter> getCounters() {
		final SortedMap<String, Counter> result = convertToDottedName(metric2Registry.getCounters());
		result.putAll(super.getCounters());
		return result;
	}

	@Override
	public SortedMap<String, Counter> getCounters(MetricFilter filter) {
		final SortedMap<String, Counter> result = convertToDottedName(metric2Registry.getMetrics(Counter.class, filter));
		result.putAll(super.getCounters(filter));
		return result;
	}

	@Override
	public SortedMap<String, Histogram> getHistograms() {
		final SortedMap<String, Histogram> result = convertToDottedName(metric2Registry.getHistograms());
		result.putAll(super.getHistograms());
		return result;
	}

	@Override
	public SortedMap<String, Histogram> getHistograms(MetricFilter filter) {
		final SortedMap<String, Histogram> result = convertToDottedName(metric2Registry.getMetrics(Histogram.class, filter));
		result.putAll(super.getHistograms(filter));
		return result;
	}

	@Override
	public SortedMap<String, Meter> getMeters() {
		final SortedMap<String, Meter> result = convertToDottedName(metric2Registry.getMeters());
		result.putAll(super.getMeters());
		return result;
	}

	@Override
	public SortedMap<String, Meter> getMeters(MetricFilter filter) {
		final SortedMap<String, Meter> result = convertToDottedName(metric2Registry.getMetrics(Meter.class, filter));
		result.putAll(super.getMeters(filter));
		return result;
	}

	@Override
	public SortedMap<String, Timer> getTimers() {
		final SortedMap<String, Timer> result = convertToDottedName(metric2Registry.getTimers());
		result.putAll(super.getTimers());
		return result;
	}

	@Override
	public SortedMap<String, Timer> getTimers(MetricFilter filter) {
		final SortedMap<String, Timer> result = convertToDottedName(metric2Registry.getMetrics(Timer.class, filter));
		result.putAll(super.getTimers(filter));
		return result;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		final SortedMap<String, Metric> result = convertToDottedName(metric2Registry.getMetrics());
		result.putAll(super.getMetrics());
		return result;
	}

	private <T> SortedMap<String, T> convertToDottedName(Map<MetricName, T> gauges) {
		SortedMap<String, T> result = new TreeMap<String, T>();
		for (Map.Entry<MetricName, T> entry : gauges.entrySet()) {
			result.put(entry.getKey().toGraphiteName(), entry.getValue());
		}
		return result;
	}

}
