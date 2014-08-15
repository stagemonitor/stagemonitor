package org.stagemonitor.core.metrics;

import com.codahale.metrics.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class SortedTableLogReporterTest {

	private Logger logger;
	private SortedTableLogReporter reporter;

	@Before
	public void setUp() throws Exception {
		logger = mock(Logger.class);
		reporter = SortedTableLogReporter
				.forRegistry(mock(MetricRegistry.class))
				.log(logger)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.NANOSECONDS)
				.filter(MetricFilter.ALL)
				.build();
	}

	@Test
	public void reportsGaugeValues() throws Exception {
		final SortedMap<String, Gauge> gauges = map("gauge3", gauge(3)).add("gaugeWithLongName1", gauge(1)).add("gauge2", gauge(2));
		reporter.report(gauges, this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Gauges ----------------------------------------------------------------------\n" +
				"name               | value\n" +
				"            gauge3 | 3\n" +
				"            gauge2 | 2\n" +
				"gaugeWithLongName1 | 1\n" +
				"\n" +
				"\n");
	}

	@Test
	public void reportsCounterValues() throws Exception {
		final Counter counter1 = mock(Counter.class);
		when(counter1.getCount()).thenReturn(100L);
		final Counter counter2 = mock(Counter.class);
		when(counter2.getCount()).thenReturn(200L);

		final SortedMap<String, Counter> counters = map("test.counter", counter1).add("test.counter2", counter2);
		reporter.report(this.<Gauge>map(), counters, this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Counters --------------------------------------------------------------------\n" +
				"name          | count\n" +
				"test.counter2 | 200\n" +
				" test.counter | 100\n" +
				"\n" +
				"\n");
	}

	@Test
	public void reportsHistogramValues() throws Exception {

		reporter.report(this.<Gauge>map(),
				this.<Counter>map(),
				map("test.histogram", histogram(3.0)).add("test.histogram2", histogram(4.0)),
				this.<Meter>map(),
				this.<Timer>map());

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Histograms ------------------------------------------------------------------\n" +
				"name            | count     | mean      | max       | min       | stddev    | p50       | p75       | p95       | p98       | p99       | p999\n" +
				"test.histogram2 |         1 |      4,00 |      4,00 |      2,00 |      5,00 |      6,00 |      7,00 |      8,00 |      9,00 |     10,00 |     11,00 | \n" +
				" test.histogram |         1 |      3,00 |      4,00 |      2,00 |      5,00 |      6,00 |      7,00 |      8,00 |      9,00 |     10,00 |     11,00 | \n" +
				"\n" +
				"\n");
	}

	private Histogram histogram(double mean) {
		final Histogram histogram = mock(Histogram.class);
		when(histogram.getCount()).thenReturn(1L);

		final Snapshot snapshot = snapshot(mean);
		when(histogram.getSnapshot()).thenReturn(snapshot);
		return histogram;
	}

	private Snapshot snapshot(double mean) {
		final Snapshot snapshot = mock(Snapshot.class);
		when(snapshot.getMax()).thenReturn(2L);
		when(snapshot.getMean()).thenReturn(mean);
		when(snapshot.getMin()).thenReturn(4L);
		when(snapshot.getStdDev()).thenReturn(5.0);
		when(snapshot.getMedian()).thenReturn(6.0);
		when(snapshot.get75thPercentile()).thenReturn(7.0);
		when(snapshot.get95thPercentile()).thenReturn(8.0);
		when(snapshot.get98thPercentile()).thenReturn(9.0);
		when(snapshot.get99thPercentile()).thenReturn(10.0);
		when(snapshot.get999thPercentile()).thenReturn(11.0);
		return snapshot;
	}

	@Test
	public void reportsMeterValues() throws Exception {

		reporter.report(this.<Gauge>map(),
				this.<Counter>map(),
				this.<Histogram>map(),
				map("test.meter1", meter(1L)).add("test.meter2", meter(2)),
				this.<Timer>map());

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Meters ----------------------------------------------------------------------\n" +
				"name        | count     | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n" +
				"test.meter2 |         2 |      2,00 |      3,00 |      4,00 |      5,00 | second        | nanoseconds\n" +
				"test.meter1 |         1 |      2,00 |      3,00 |      4,00 |      5,00 | second        | nanoseconds\n" +
				"\n" +
				"\n");
	}

	private Meter meter(long count) {
		final Meter meter = mock(Meter.class);
		when(meter.getCount()).thenReturn(count);
		when(meter.getMeanRate()).thenReturn(2.0);
		when(meter.getOneMinuteRate()).thenReturn(3.0);
		when(meter.getFiveMinuteRate()).thenReturn(4.0);
		when(meter.getFifteenMinuteRate()).thenReturn(5.0);
		return meter;
	}

	@Test
	public void reportsTimerValues() throws Exception {

		reporter.report(this.<Gauge>map(),
				this.<Counter>map(),
				this.<Histogram>map(),
				this.<Meter>map(),
				map("timer1", timer(4)).add("timer2", timer(2)).add("timer3", timer(3)));

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Timers ----------------------------------------------------------------------\n" +
				"name   | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n" +
				"timer1 |         1 |      4,00 |      4,00 |      2,00 |      5,00 |      6,00 |      7,00 |      8,00 |      9,00 |     10,00 |     11,00 |      2,00 |      3,00 |      4,00 |      5,00 | second        | nanoseconds\n" +
				"timer3 |         1 |      3,00 |      4,00 |      2,00 |      5,00 |      6,00 |      7,00 |      8,00 |      9,00 |     10,00 |     11,00 |      2,00 |      3,00 |      4,00 |      5,00 | second        | nanoseconds\n" +
				"timer2 |         1 |      2,00 |      4,00 |      2,00 |      5,00 |      6,00 |      7,00 |      8,00 |      9,00 |     10,00 |     11,00 |      2,00 |      3,00 |      4,00 |      5,00 | second        | nanoseconds\n" +
				"\n" +
				"\n");
	}

	private Timer timer(double mean) {
		final Timer timer = mock(Timer.class);
		when(timer.getCount()).thenReturn(1L);
		when(timer.getMeanRate()).thenReturn(2.0);
		when(timer.getOneMinuteRate()).thenReturn(3.0);
		when(timer.getFiveMinuteRate()).thenReturn(4.0);
		when(timer.getFifteenMinuteRate()).thenReturn(5.0);
		final Snapshot snapshot = snapshot(mean);
		when(timer.getSnapshot()).thenReturn(snapshot);
		return timer;
	}

	private <T> SortedMap<String, T> map() {
		return new TreeMap<String, T>();
	}

	private <T> FluentMap<String, T> map(String name, T metric) {
		final FluentMap<String, T> map = new FluentMap<String, T>();
		map.put(name, metric);
		return map;
	}

	private <T> Gauge gauge(T value) {
		final Gauge gauge = mock(Gauge.class);
		when(gauge.getValue()).thenReturn(value);
		return gauge;
	}

	private class FluentMap<K, V> extends TreeMap<K, V> {
		public FluentMap<K, V> add(K key, V value) {
			this.put(key, value);
			return this;
		}
	}
}
