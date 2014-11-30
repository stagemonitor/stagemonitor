package org.stagemonitor.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsReporterTestHelper {

	public static Histogram histogram(double mean) {
		return histogram(1L, snapshot(mean));
	}

	public static Histogram histogram(long count, Snapshot snapshot) {
		final Histogram histogram = mock(Histogram.class);
		when(histogram.getCount()).thenReturn(count);
		when(histogram.getSnapshot()).thenReturn(snapshot);
		return histogram;
	}

	public static Snapshot snapshot(double mean) {
		return snapshot(mean, 2L, 4L, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0);
	}

	public static Snapshot snapshot(double mean, long max, long min, double stdDev, double median, double p75, double p95,
								double p98, double p99, double p999) {
		final Snapshot snapshot = mock(Snapshot.class);
		when(snapshot.getMax()).thenReturn(max);
		when(snapshot.getMean()).thenReturn(mean);
		when(snapshot.getMin()).thenReturn(min);
		when(snapshot.getStdDev()).thenReturn(stdDev);
		when(snapshot.getMedian()).thenReturn(median);
		when(snapshot.get75thPercentile()).thenReturn(p75);
		when(snapshot.get95thPercentile()).thenReturn(p95);
		when(snapshot.get98thPercentile()).thenReturn(p98);
		when(snapshot.get99thPercentile()).thenReturn(p99);
		when(snapshot.get999thPercentile()).thenReturn(p999);
		return snapshot;
	}

	public static Meter meter(long count) {
		final Meter meter = mock(Meter.class);
		when(meter.getCount()).thenReturn(count);
		when(meter.getMeanRate()).thenReturn(2.0);
		when(meter.getOneMinuteRate()).thenReturn(3.0);
		when(meter.getFiveMinuteRate()).thenReturn(4.0);
		when(meter.getFifteenMinuteRate()).thenReturn(5.0);
		return meter;
	}

	public static Timer timer(double mean) {
		return timer(1L, 2.0, 3.0, 4.0, 5.0, snapshot(mean));
	}

	public static Timer timer(long count, double meanRate, double m1Rate, double m5Rate, double m15Rate, Snapshot snapshot) {
		final Timer timer = mock(Timer.class);
		when(timer.getCount()).thenReturn(count);
		when(timer.getMeanRate()).thenReturn(meanRate);
		when(timer.getOneMinuteRate()).thenReturn(m1Rate);
		when(timer.getFiveMinuteRate()).thenReturn(m5Rate);
		when(timer.getFifteenMinuteRate()).thenReturn(m15Rate);
		when(timer.getSnapshot()).thenReturn(snapshot);
		return timer;
	}

	public static <T> SortedMap<String, T> map() {
		return new TreeMap<String, T>();
	}

	public static <T> SortedTableLogReporterTest.FluentMap<String, T> map(String name, T metric) {
		final SortedTableLogReporterTest.FluentMap<String, T> map = new SortedTableLogReporterTest.FluentMap<String, T>();
		map.put(name, metric);
		return map;
	}

	public static <T> Gauge gauge(T value) {
		final Gauge gauge = mock(Gauge.class);
		when(gauge.getValue()).thenReturn(value);
		return gauge;
	}

	public static Counter counter(long count) {
		final Counter counter = mock(Counter.class);
		when(counter.getCount()).thenReturn(count);
		return counter;
	}

	public static class FluentMap<K, V> extends TreeMap<K, V> {
		public FluentMap<K, V> add(K key, V value) {
			this.put(key, value);
			return this;
		}
	}
}
