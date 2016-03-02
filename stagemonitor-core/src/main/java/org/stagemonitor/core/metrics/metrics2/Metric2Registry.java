package org.stagemonitor.core.metrics.metrics2;

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A metrics registry that does not use a simple dotted metric name but a key-value pair for the metric identity
 */
public class Metric2Registry implements Metric2Set {

	private final ConcurrentMap<MetricName, Metric> metrics;
	
	// An internal reference to a legacy Dropwizard Metric Registry
	// that we echo registration/removal of Metrics to so that it
	// matches the state of our registry and supports the Dropwizard
	// Metrics listener and reporting patterns.
	private final MetricRegistry metricRegistry;

	public Metric2Registry() {
		this(new ConcurrentHashMap<MetricName, Metric>());
	}

	public Metric2Registry(ConcurrentMap<MetricName, Metric> metrics) {
		this.metrics = metrics;
		this.metricRegistry = new MetricRegistry();
	}

	/**
	 * Given a {@link Metric}, registers it under the given name.
	 *
	 * @param name   the name of the metric
	 * @param metric the metric
	 * @param <T>    the type of the metric
	 * @return {@code metric}
	 * @throws IllegalArgumentException if the name is already registered
	 */
	@SuppressWarnings("unchecked")
	public <T extends Metric> T register(MetricName name, T metric) throws IllegalArgumentException {
		if (metric instanceof MetricSet) {
			throw new IllegalArgumentException("This metrics registry is not compatible with MetricSets. Use a Metric2Set instead.");
		} else {
			final Metric existing = metrics.putIfAbsent(name, metric);
			if (existing != null) {
				throw new IllegalArgumentException("A metric named " + name + " already exists");
			}
			else {
				// This is a new metric - we have to register the Metric with
				// the legacy Dropwizard Metric registry as
				// well to support existing reports and listeners
				metricRegistry.register(name.toGraphiteName(), metric);
			}
		}
		return metric;
	}

	/**
	 * Given a metric set, registers them.
	 *
	 * @param metrics a set of metrics
	 * @throws IllegalArgumentException if any of the names are already registered
	 */
	public void registerAll(Metric2Set metrics) throws IllegalArgumentException {
		for (Map.Entry<MetricName, Metric> entry : metrics.getMetrics().entrySet()) {
			register(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * Given a metric set, registers the ones not already registered.
	 * This method prevents IllegalArgumentException
	 * @param metrics a set of metrics
	 */
	public void registerAny(Metric2Set metrics) throws IllegalArgumentException {
		final Set<MetricName> registeredNames = getNames();
		for (Map.Entry<MetricName, Metric> entry : metrics.getMetrics().entrySet()) {
			if (!registeredNames.contains(entry.getKey())) {
				register(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Return the {@link Counter} registered under this name; or create and register
	 * a new {@link Counter} if none is registered.
	 *
	 * @param name the name of the metric
	 * @return a new or pre-existing {@link Counter}
	 */
	public Counter counter(MetricName name) {
		return getOrAdd(name, MetricBuilder.COUNTERS);
	}

	/**
	 * Return the {@link Histogram} registered under this name; or create and register
	 * a new {@link Histogram} if none is registered.
	 *
	 * @param name the name of the metric
	 * @return a new or pre-existing {@link Histogram}
	 */
	public Histogram histogram(MetricName name) {
		return getOrAdd(name, MetricBuilder.HISTOGRAMS);
	}

	/**
	 * Return the {@link Meter} registered under this name; or create and register
	 * a new {@link Meter} if none is registered.
	 *
	 * @param name the name of the metric
	 * @return a new or pre-existing {@link Meter}
	 */
	public Meter meter(MetricName name) {
		return getOrAdd(name, MetricBuilder.METERS);
	}

	/**
	 * Return the {@link Timer} registered under this name; or create and register
	 * a new {@link Timer} if none is registered.
	 *
	 * @param name the name of the metric
	 * @return a new or pre-existing {@link Timer}
	 */
	public Timer timer(MetricName name) {
		return getOrAdd(name, MetricBuilder.TIMERS);
	}

	/**
	 * Removes the metric with the given name.
	 *
	 * @param name the name of the metric
	 * @return whether or not the metric was removed
	 */
	public boolean remove(MetricName name) {
		final Metric metric = metrics.remove(name);
		if (metric != null) {
			// We have to unregister the Metric with the legacy Dropwizard Metric registry as
			// well to support existing reports and listeners
			metricRegistry.remove(name.toGraphiteName());
			return true;
		}
		return false;
	}

	/**
	 * Returns a set of the names of all the metrics in the registry.
	 *
	 * @return the names of all the metrics
	 */
	public Set<MetricName> getNames() {
		return Collections.unmodifiableSet(new HashSet<MetricName>(metrics.keySet()));
	}

	/**
	 * Returns a map of all the gauges in the registry and their names.
	 *
	 * @return all the gauges in the registry
	 */
	public Map<MetricName, Gauge> getGauges() {
		return getGauges(Metric2Filter.ALL);
	}

	/**
	 * Returns a map of all the gauges in the registry and their names which match the given filter.
	 *
	 * @param filter the metric filter to match
	 * @return all the gauges in the registry
	 */
	public Map<MetricName, Gauge> getGauges(Metric2Filter filter) {
		return getMetrics(Gauge.class, filter);
	}

	/**
	 * Returns a map of all the counters in the registry and their names.
	 *
	 * @return all the counters in the registry
	 */
	public Map<MetricName, Counter> getCounters() {
		return getCounters(Metric2Filter.ALL);
	}

	/**
	 * Returns a map of all the counters in the registry and their names which match the given
	 * filter.
	 *
	 * @param filter the metric filter to match
	 * @return all the counters in the registry
	 */
	public Map<MetricName, Counter> getCounters(Metric2Filter filter) {
		return getMetrics(Counter.class, filter);
	}

	/**
	 * Returns a map of all the histograms in the registry and their names.
	 *
	 * @return all the histograms in the registry
	 */
	public Map<MetricName, Histogram> getHistograms() {
		return getHistograms(Metric2Filter.ALL);
	}

	/**
	 * Returns a map of all the histograms in the registry and their names which match the given
	 * filter.
	 *
	 * @param filter the metric filter to match
	 * @return all the histograms in the registry
	 */
	public Map<MetricName, Histogram> getHistograms(Metric2Filter filter) {
		return getMetrics(Histogram.class, filter);
	}

	/**
	 * Returns a map of all the meters in the registry and their names.
	 *
	 * @return all the meters in the registry
	 */
	public Map<MetricName, Meter> getMeters() {
		return getMeters(Metric2Filter.ALL);
	}

	/**
	 * Returns a map of all the meters in the registry and their names which match the given filter.
	 *
	 * @param filter the metric filter to match
	 * @return all the meters in the registry
	 */
	public Map<MetricName, Meter> getMeters(Metric2Filter filter) {
		return getMetrics(Meter.class, filter);
	}

	/**
	 * Returns a map of all the timers in the registry and their names.
	 *
	 * @return all the timers in the registry
	 */
	public Map<MetricName, Timer> getTimers() {
		return getTimers(Metric2Filter.ALL);
	}

	/**
	 * Returns a map of all the timers in the registry and their names which match the given filter.
	 *
	 * @param filter the metric filter to match
	 * @return all the timers in the registry
	 */
	public Map<MetricName, Timer> getTimers(Metric2Filter filter) {
		return getMetrics(Timer.class, filter);
	}

	@SuppressWarnings("unchecked")
	private <T extends Metric> T getOrAdd(MetricName name, MetricBuilder<T> builder) {
		final Metric metric = metrics.get(name);
		if (builder.isInstance(metric)) {
			return (T) metric;
		} else if (metric == null) {
			try {
				return register(name, builder.newMetric());
			} catch (IllegalArgumentException e) {
				final Metric added = metrics.get(name);
				if (builder.isInstance(added)) {
					return (T) added;
				}
			}
		}
		throw new IllegalArgumentException(name + " is already used for a different type of metric");
	}

	@SuppressWarnings("unchecked")
	private <T extends Metric> Map<MetricName, T> getMetrics(Class<T> klass, Metric2Filter filter) {
		final Map<MetricName, T> metrics = new HashMap<MetricName, T>();
		for (Map.Entry<MetricName, Metric> entry : this.metrics.entrySet()) {
			if (klass.isInstance(entry.getValue()) && filter.matches(entry.getKey(), entry.getValue())) {
				metrics.put(entry.getKey(), (T) entry.getValue());
			}
		}
		return Collections.unmodifiableMap(metrics);
	}

	@SuppressWarnings("unchecked")
	protected <T extends Metric> Map<MetricName, T> getMetrics(Class<T> klass, MetricFilter filter) {
		final Map<MetricName, T> metrics = new HashMap<MetricName, T>();
		for (Map.Entry<MetricName, Metric> entry : this.metrics.entrySet()) {
			if (klass.isInstance(entry.getValue()) && filter.matches(entry.getKey().toGraphiteName(), entry.getValue())) {
				metrics.put(entry.getKey(), (T) entry.getValue());
			}
		}
		return Collections.unmodifiableMap(metrics);
	}

	@Override
	public Map<MetricName, Metric> getMetrics() {
		return Collections.unmodifiableMap(metrics);
	}

	/**
	 * Removes all metrics which match the given filter.
	 *
	 * @param filter a filter
	 */
	public void removeMatching(MetricFilter filter) {
		for (Map.Entry<MetricName, Metric> entry : metrics.entrySet()) {
			if (filter.matches(entry.getKey().toGraphiteName(), entry.getValue())) {
				remove(entry.getKey());
			}
		}
	}

	/**
	 * Removes all metrics which match the given filter.
	 *
	 * @param filter a filter
	 */
	public void removeMatching(Metric2Filter filter) {
		for (Map.Entry<MetricName, Metric> entry : metrics.entrySet()) {
			if (filter.matches(entry.getKey(), entry.getValue())) {
				remove(entry.getKey());
			}
		}
	}

	/**
	 * A quick and easy way of capturing the notion of default metrics.
	 */
	private interface MetricBuilder<T extends Metric> {
		MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
			@Override
			public Counter newMetric() {
				return new Counter();
			}

			@Override
			public boolean isInstance(Metric metric) {
				return Counter.class.isInstance(metric);
			}
		};

		MetricBuilder<Histogram> HISTOGRAMS = new MetricBuilder<Histogram>() {
			@Override
			public Histogram newMetric() {
				return new Histogram(new ExponentiallyDecayingReservoir());
			}

			@Override
			public boolean isInstance(Metric metric) {
				return Histogram.class.isInstance(metric);
			}
		};

		MetricBuilder<Meter> METERS = new MetricBuilder<Meter>() {
			@Override
			public Meter newMetric() {
				return new Meter();
			}

			@Override
			public boolean isInstance(Metric metric) {
				return Meter.class.isInstance(metric);
			}
		};

		MetricBuilder<Timer> TIMERS = new MetricBuilder<Timer>() {
			@Override
			public Timer newMetric() {
				return new Timer();
			}

			@Override
			public boolean isInstance(Metric metric) {
				return Timer.class.isInstance(metric);
			}
		};

		T newMetric();

		boolean isInstance(Metric metric);
	}

	/**
	 * Returns the wrapped legacy {@link MetricRegistry}
	 *
	 * @return the wrapped legacy {@link MetricRegistry}
	 */
	public MetricRegistry getMetricRegistry() {
		return metricRegistry;
	}

}
