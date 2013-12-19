package de.isys.jawap.collector.core.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static de.isys.jawap.collector.core.monitor.ExecutionContextMonitor.decodeForGraphite;

public class SortedTableConsoleReporter extends ScheduledReporter {
	/**
	 * Returns a new {@link SortedTableConsoleReporter.Builder} for {@link SortedTableConsoleReporter}.
	 *
	 * @param registry the registry to report
	 * @return a {@link SortedTableConsoleReporter.Builder} instance for a {@link SortedTableConsoleReporter}
	 */
	public static Builder forRegistry(MetricRegistry registry) {
		return new Builder(registry);
	}

	/**
	 * A builder for {@link SortedTableConsoleReporter} instances. Defaults to using the
	 * default locale and time zone, writing to {@code System.out}, converting
	 * rates to events/second, converting durations to milliseconds, and not
	 * filtering metrics.
	 */
	public static class Builder {
		private final MetricRegistry registry;
		private PrintStream output;
		private Locale locale;
		private Clock clock;
		private TimeZone timeZone;
		private TimeUnit rateUnit;
		private TimeUnit durationUnit;
		private MetricFilter filter;

		private Builder(MetricRegistry registry) {
			this.registry = registry;
			this.output = System.out;
			this.locale = Locale.getDefault();
			this.clock = Clock.defaultClock();
			this.timeZone = TimeZone.getDefault();
			this.rateUnit = TimeUnit.SECONDS;
			this.durationUnit = TimeUnit.MILLISECONDS;
			this.filter = MetricFilter.ALL;
		}

		/**
		 * Write to the given {@link java.io.PrintStream}.
		 *
		 * @param output a {@link java.io.PrintStream} instance.
		 * @return {@code this}
		 */
		public Builder outputTo(PrintStream output) {
			this.output = output;
			return this;
		}

		/**
		 * Format numbers for the given {@link java.util.Locale}.
		 *
		 * @param locale a {@link java.util.Locale}
		 * @return {@code this}
		 */
		public Builder formattedFor(Locale locale) {
			this.locale = locale;
			return this;
		}

		/**
		 * Use the given {@link com.codahale.metrics.Clock} instance for the time.
		 *
		 * @param clock a {@link com.codahale.metrics.Clock} instance
		 * @return {@code this}
		 */
		public Builder withClock(Clock clock) {
			this.clock = clock;
			return this;
		}

		/**
		 * Use the given {@link java.util.TimeZone} for the time.
		 *
		 * @param timeZone a {@link java.util.TimeZone}
		 * @return {@code this}
		 */
		public Builder formattedFor(TimeZone timeZone) {
			this.timeZone = timeZone;
			return this;
		}

		/**
		 * Convert rates to the given time unit.
		 *
		 * @param rateUnit a unit of time
		 * @return {@code this}
		 */
		public Builder convertRatesTo(TimeUnit rateUnit) {
			this.rateUnit = rateUnit;
			return this;
		}

		/**
		 * Convert durations to the given time unit.
		 *
		 * @param durationUnit a unit of time
		 * @return {@code this}
		 */
		public Builder convertDurationsTo(TimeUnit durationUnit) {
			this.durationUnit = durationUnit;
			return this;
		}

		/**
		 * Only report metrics which match the given filter.
		 *
		 * @param filter a {@link com.codahale.metrics.MetricFilter}
		 * @return {@code this}
		 */
		public Builder filter(MetricFilter filter) {
			this.filter = filter;
			return this;
		}

		/**
		 * Builds a {@link SortedTableConsoleReporter} with the given properties.
		 *
		 * @return a {@link SortedTableConsoleReporter}
		 */
		public SortedTableConsoleReporter build() {
			return new SortedTableConsoleReporter(registry, output, locale, clock, timeZone, rateUnit, durationUnit, filter);
		}
	}

	private static final int CONSOLE_WIDTH = 80;

	private final PrintStream output;
	private final Locale locale;
	private final Clock clock;
	private final DateFormat dateFormat;

	private SortedTableConsoleReporter(MetricRegistry registry, PrintStream output, Locale locale, Clock clock, TimeZone timeZone, TimeUnit rateUnit,
									   TimeUnit durationUnit, MetricFilter filter) {
		super(registry, "console-reporter", filter, rateUnit, durationUnit);
		this.output = output;
		this.locale = locale;
		this.clock = clock;
		this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
		dateFormat.setTimeZone(timeZone);
	}

	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms,
					   SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		final String dateTime = dateFormat.format(new Date(clock.getTime()));
		printWithBanner(dateTime, '=');
		output.println();

		if (!gauges.isEmpty()) {
			printWithBanner("-- Gauges", '-');
			int maxLength = getMaxLengthOfKeys(gauges);
			output.printf("%-" + maxLength + "s | value\n", "name");
			Map<String, Gauge> sortedTimers = sortByValue(gauges, new Comparator<Gauge>() {
				private NaturalOrderComparator<Object> naturalOrderComparator = new NaturalOrderComparator<Object>();

				@Override
				public int compare(Gauge o1, Gauge o2) {
					return naturalOrderComparator.compare(o2.getValue(), o1.getValue());
				}
			});
			for (Map.Entry<String, Gauge> entry : sortedTimers.entrySet()) {
				printGauge(entry.getKey(), entry.getValue(), maxLength);
			}
			output.println();
		}
		if (!counters.isEmpty()) {
			printWithBanner("-- Counters", '-');
			int maxLength = getMaxLengthOfKeys(counters);
			output.printf("%-" + maxLength + "s | count\n", "name");
			Map<String, Counter> sortedTimers = sortByValue(counters, new Comparator<Counter>() {
				@Override
				public int compare(Counter o1, Counter o2) {
					return Long.compare(o2.getCount(), o1.getCount());
				}
			});
			for (Map.Entry<String, Counter> entry : sortedTimers.entrySet()) {
				printCounter(entry.getKey(), entry.getValue(), maxLength);
			}
			output.println();
		}

		if (!histograms.isEmpty()) {
			printWithBanner("-- Histograms", '-');
			int maxLength = getMaxLengthOfKeys(histograms);
			output.printf("%-" + maxLength + "s | count     | max       | mean      | min       | stddev    | p50       | p75       | p95       | p98       | p99       | p999\n", "name");
			Map<String, Histogram> sortedTimers = sortByValue(histograms, new Comparator<Histogram>() {
				@Override
				public int compare(Histogram o1, Histogram o2) {
					return Double.compare(o2.getSnapshot().getMean(), o1.getSnapshot().getMean());
				}
			});
			for (Map.Entry<String, Histogram> entry : sortedTimers.entrySet()) {
				printHistogram(entry.getKey(), entry.getValue(), maxLength);
			}
			output.println();
		}

		if (!meters.isEmpty()) {
			printWithBanner("-- Meters", '-');
			int maxLength = getMaxLengthOfKeys(meters);
			output.printf("%-" + maxLength + "s | count     | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit\n", "name");
			Map<String, Meter> sortedTimers = sortByValue(meters, new Comparator<Meter>() {
				@Override
				public int compare(Meter o1, Meter o2) {
					return Long.compare(o2.getCount(), o1.getCount());
				}
			});
			for (Map.Entry<String, Meter> entry : sortedTimers.entrySet()) {
				printMeter(entry.getKey(), entry.getValue(), maxLength);
			}
			output.println();
		}

		if (!timers.isEmpty()) {
			printWithBanner("-- Timers", '-');
			int maxLength = getMaxLengthOfKeys(timers);
			output.printf("%-" + maxLength + "s | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n", "name");
			Map<String, Timer> sortedTimers = sortByValue(timers, new Comparator<Timer>() {
				public int compare(Timer o1, Timer o2) {
					return Double.compare(o2.getSnapshot().getMean(), o1.getSnapshot().getMean());
				}
			});
			for (Map.Entry<String, Timer> entry : sortedTimers.entrySet()) {
				printTimer(entry.getKey(), entry.getValue(), maxLength);
			}
			output.println();
		}

		output.println();
		output.flush();
	}


	private class NaturalOrderComparator<T> implements Comparator<T> {
		@Override
		public int compare(T o1, T o2) {
			if (o1 instanceof Comparable) {
				return ((Comparable<T>) o1).compareTo(o2);
			}
			return 0;
		}
	}

	private static <K, V> Map<K, V> sortByValue(Map<K, V> map, final Comparator<V> valueComparator) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (valueComparator.compare(o1.getValue(), o2.getValue()));
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private static int getMaxLengthOfKeys(Map<String, ?> map) {
		int maxLength = -1;
		for (String s : map.keySet()) {
			if (s.length() > maxLength) {
				maxLength = removePrefix(s).length();
			}
		}
		return maxLength;
	}

	private void printGauge(String name, Gauge gauge, int maxNameLength) {
		output.printf("%" + maxNameLength + "s | ", removePrefix(decodeForGraphite(name)));
		output.println(gauge.getValue());

	}

	private static String removePrefix(String s) {
		return s.replaceFirst("^.*?\\.", "");
	}

	private void printCounter(String name, Counter counter, int maxNameLength) {
		output.printf("%" + maxNameLength + "s | ", removePrefix(decodeForGraphite(name)));
		output.println(counter.getCount());
	}

	private void printMeter(String name, Meter meter, int maxNameLength) {
		output.printf("%" + maxNameLength + "s | ", removePrefix(decodeForGraphite(name)));
		output.println(meter.getCount());
		printMetered(meter);
		output.println();
	}

	private void printMetered(Metered metered) {
		printDouble(convertRate(metered.getMeanRate()));
		printDouble(convertRate(metered.getOneMinuteRate()));
		printDouble(convertRate(metered.getFiveMinuteRate()));
		printDouble(convertRate(metered.getFifteenMinuteRate()));
		output.printf("%-13s | ", getRateUnit());
		output.print(getDurationUnit());
	}


	private void printHistogram(String name, Histogram histogram, int maxNameLength) {
		output.printf("%" + maxNameLength + "s | ", removePrefix(decodeForGraphite(name)));
		output.println(histogram.getCount());
		printSnapshot(histogram.getSnapshot());
		output.println();
	}

	private void printSnapshot(Snapshot snapshot) {
		printDouble(convertDuration(snapshot.getMean()));
		printDouble(convertDuration(snapshot.getMin()));
		printDouble(convertDuration(snapshot.getMax()));
		printDouble(convertDuration(snapshot.getStdDev()));
		printDouble(convertDuration(snapshot.getMedian()));
		printDouble(convertDuration(snapshot.get75thPercentile()));
		printDouble(convertDuration(snapshot.get95thPercentile()));
		printDouble(convertDuration(snapshot.get98thPercentile()));
		printDouble(convertDuration(snapshot.get99thPercentile()));
		printDouble(convertDuration(snapshot.get999thPercentile()));
	}

	private void printTimer(String name, Timer timer, int maxNameLength) {
		final Snapshot snapshot = timer.getSnapshot();
		output.printf("%" + maxNameLength + "s | ", removePrefix(decodeForGraphite(name)));
		output.printf(locale, "%,9d | ", timer.getCount());
		printSnapshot(snapshot);
		printMetered(timer);
		output.println();
	}

	public void printDouble(double d) {
		output.printf(locale, "%,9.2f | ", d);
	}

	private void printWithBanner(String s, char c) {
		output.print(s);
		output.print(' ');
		for (int i = 0; i < (CONSOLE_WIDTH - s.length() - 1); i++) {
			output.print(c);
		}
		output.println();
	}

}
