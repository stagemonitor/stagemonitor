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
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;

public class MetricRegistryAdapter extends MetricRegistry {

	private static final String MESSAGE = "This is a read only MetricRegistry";

	private final MetricRegistry legacyRegistry;

	private final Metric2Registry metric2Registry;

	public MetricRegistryAdapter(MetricRegistry legacyRegistry, Metric2Registry metric2Registry) {
		this.legacyRegistry = legacyRegistry;
		this.metric2Registry = metric2Registry;
	}

	@Override
	public SortedSet<String> getNames() {
		SortedSet<String> result = new TreeSet<String>();
		for (MetricName metricName : metric2Registry.getNames()) {
			result.add(metricName.toGraphiteName());
		}
		result.addAll(legacyRegistry.getNames());
		return result;
	}

	@Override
	public SortedMap<String, Gauge> getGauges() {
		final SortedMap<String, Gauge> result = convertToDottedName(metric2Registry.getGauges());
		result.putAll(legacyRegistry.getGauges());
		return result;
	}

	@Override
	public SortedMap<String, Gauge> getGauges(MetricFilter filter) {
		final SortedMap<String, Gauge> result = convertToDottedName(metric2Registry.getMetrics(Gauge.class, filter));
		result.putAll(legacyRegistry.getGauges(filter));
		return result;
	}

	@Override
	public SortedMap<String, Counter> getCounters() {
		final SortedMap<String, Counter> result = convertToDottedName(metric2Registry.getCounters());
		result.putAll(legacyRegistry.getCounters());
		return result;
	}

	@Override
	public SortedMap<String, Counter> getCounters(MetricFilter filter) {
		final SortedMap<String, Counter> result = convertToDottedName(metric2Registry.getMetrics(Counter.class, filter));
		result.putAll(legacyRegistry.getCounters(filter));
		return result;
	}

	@Override
	public SortedMap<String, Histogram> getHistograms() {
		final SortedMap<String, Histogram> result = convertToDottedName(metric2Registry.getHistograms());
		result.putAll(legacyRegistry.getHistograms());
		return result;
	}

	@Override
	public SortedMap<String, Histogram> getHistograms(MetricFilter filter) {
		final SortedMap<String, Histogram> result = convertToDottedName(metric2Registry.getMetrics(Histogram.class, filter));
		result.putAll(legacyRegistry.getHistograms(filter));
		return result;
	}

	@Override
	public SortedMap<String, Meter> getMeters() {
		final SortedMap<String, Meter> result = convertToDottedName(metric2Registry.getMeters());
		result.putAll(legacyRegistry.getMeters());
		return result;
	}

	@Override
	public SortedMap<String, Meter> getMeters(MetricFilter filter) {
		final SortedMap<String, Meter> result = convertToDottedName(metric2Registry.getMetrics(Meter.class, filter));
		result.putAll(legacyRegistry.getMeters(filter));
		return result;
	}

	@Override
	public SortedMap<String, Timer> getTimers() {
		final SortedMap<String, Timer> result = convertToDottedName(metric2Registry.getTimers());
		result.putAll(legacyRegistry.getTimers());
		return result;
	}

	@Override
	public SortedMap<String, Timer> getTimers(MetricFilter filter) {
		final SortedMap<String, Timer> result = convertToDottedName(metric2Registry.getMetrics(Timer.class, filter));
		result.putAll(legacyRegistry.getTimers(filter));
		return result;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		final SortedMap<String, Metric> result = convertToDottedName(metric2Registry.getMetrics());
		result.putAll(legacyRegistry.getMetrics());
		return result;
	}

	@Override
	public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public void registerAll(MetricSet metrics) throws IllegalArgumentException {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Counter counter(String name) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Histogram histogram(String name) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Meter meter(String name) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Timer timer(String name) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public boolean remove(String name) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public void removeMatching(MetricFilter filter) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public void addListener(MetricRegistryListener listener) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public void removeListener(MetricRegistryListener listener) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	private <T> SortedMap<String, T> convertToDottedName(Map<MetricName, T> gauges) {
		SortedMap<String, T> result = new TreeMap<String, T>();
		for (Map.Entry<MetricName, T> entry : gauges.entrySet()) {
			result.put(entry.getKey().toGraphiteName(), entry.getValue());
		}
		return result;
	}

}
