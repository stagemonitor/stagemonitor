package org.stagemonitor.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MetricsAggregationReporterTest extends AbstractMetricsReporterTest {

	private MetricsAggregationReporter reporter;
	private Logger logger;
	private SortedTableLogReporter onShutdownReporter;

	@Before
	public void setUp() throws Exception {

		logger = mock(Logger.class);
		onShutdownReporter = SortedTableLogReporter
				.forRegistry(mock(MetricRegistry.class))
				.log(logger)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.NANOSECONDS)
				.filter(MetricFilter.ALL)
				.formattedFor(Locale.US)
				.build();
		reporter = new MetricsAggregationReporter(new MetricRegistry(), MetricFilter.ALL,
				Collections.<ScheduledReporter>singletonList(onShutdownReporter));
	}

	@Test
	public void testComputeAverage() throws Exception {
		long[] measurements = new long[]{12, 3, 43, 5, 6, 56, 745, 34, 23, 32, 432, 3};

		double avg = 0;
		for (int i = 0; i < measurements.length; i++) {
			avg = MetricsAggregationReporter.computeMovingAverage(avg, i, measurements[i]);
		}

		Assert.assertEquals(getAverage(measurements), avg, 0.000000000001);
	}

	private double getAverage(long[] measurements) {
		double sum = 0;
		for (long measurement : measurements) {
			sum += measurement;
		}
		return sum / measurements.length;
	}

	@Test
	public void testReportGauges() throws Exception {
		reporter.report(map("string", gauge("foo")).add("double", gauge(2.5d)).add("BigInteger", gauge(new BigInteger("2"))),
				this.<Counter>map(),
				this.<Histogram>map(),
				this.<Meter>map(),
				this.<Timer>map());
		reporter.report(map("string", gauge("bar")).add("double", gauge(2.5d * 3)).add("BigInteger", gauge(new BigInteger("6"))),
				this.<Counter>map(),
				this.<Histogram>map(),
				this.<Meter>map(),
				this.<Timer>map());

		reporter.onShutDown();

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Gauges ----------------------------------------------------------------------\n" +
				"name       | value\n" +
				"    string | bar\n" +
				"    double | 5.0\n" +
				"BigInteger | 4.0\n" +
				"\n" +
				"\n");
	}

	@Test
	public void testReportCounters() throws Exception {
		reporter.report(this.<Gauge>map(),
				map("c1", counter(2)),
				this.<Histogram>map(),
				this.<Meter>map(),
				this.<Timer>map());

		reporter.report(this.<Gauge>map(),
				map("counter1", counter(3)).add("counter2", counter(5)),
				this.<Histogram>map(),
				this.<Meter>map(),
				this.<Timer>map());

		reporter.onShutDown();
		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Counters --------------------------------------------------------------------\n" +
				"name     | count\n" +
				"counter2 | 5\n" +
				"counter1 | 3\n" +
				"\n" +
				"\n");
	}

	@Test
	public void testReportHistograms() throws Exception {
		reporter.report(this.<Gauge>map(),
				this.<Counter>map(),
				map("histogram", histogram(1, snapshot(4, 11L, 2L, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0))),
				this.<Meter>map(),
				this.<Timer>map());

		reporter.report(this.<Gauge>map(),
				this.<Counter>map(),
				map("histogram", histogram(1 * 3, snapshot(4 * 3, 11L * 3, 2L * 3, 5.0 * 3, 6.0 * 3, 7.0 * 3, 8.0 * 3, 9.0 * 3, 10.0 * 3, 11.0 * 3))),
				this.<Meter>map(),
				this.<Timer>map());

		reporter.onShutDown();

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Histograms ------------------------------------------------------------------\n" +
				"name      | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      |\n" +
				"histogram |         3 |      8.00 |      2.00 |     33.00 |     10.00 |     12.00 |     14.00 |     16.00 |     18.00 |     20.00 |     22.00 | \n\n\n");

	}

	@Test
	public void reportsMeterValues() throws Exception {

		reporter.report(this.<Gauge>map(),
				this.<Counter>map(),
				this.<Histogram>map(),
				map("test.meter1", meter(1L)).add("test.meter2", meter(2)),
				this.<Timer>map());

		reporter.onShutDown();

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Meters ----------------------------------------------------------------------\n" +
				"name        | count     | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n" +
				"test.meter2 |         2 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"test.meter1 |         1 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"\n" +
				"\n");
	}

	@Test
	public void testReportTimers() throws Exception {
		reporter.report(this.<Gauge>map(),
				this.<Counter>map(),
				this.<Histogram>map(),
				this.<Meter>map(),
				map("timer1", timer(1L, 2.0, 3.0, 4.0, 5.0, snapshot(4, 11L, 2L, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0))));

		reporter.report(this.<Gauge>map(),
				this.<Counter>map(),
				this.<Histogram>map(),
				this.<Meter>map(),
				map("timer1", timer(4L, 2.0 * 3, 3.0 * 3, 4.0 * 3, 5.0 * 3, snapshot(4 * 3, 11L * 3, 1, 5.0 * 3, 6.0 * 3, 7.0 * 3, 8.0 * 3, 9.0 * 3, 10.0 * 3, 11.0 * 3))));

		reporter.onShutDown();

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Timers ----------------------------------------------------------------------\n" +
				"name   | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n" +
				"timer1 |         4 |      8.00 |      1.00 |     33.00 |     10.00 |     12.00 |     14.00 |     16.00 |     18.00 |     20.00 |     22.00 |      6.00 |      6.00 |      6.00 |      6.00 | second        | nanoseconds\n\n\n");

	}
}
