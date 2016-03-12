package org.stagemonitor.core.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.counter;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.gauge;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.histogram;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.map;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.meter;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.snapshot;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.timer;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.math.BigInteger;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class MetricsAggregationReporterTest {

	private MetricsAggregationReporter reporter;
	private Logger logger;

	@Before
	public void setUp() throws Exception {

		logger = mock(Logger.class);
		SortedTableLogReporter onShutdownReporter = SortedTableLogReporter
				.forRegistry(mock(Metric2Registry.class))
				.log(logger)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.NANOSECONDS)
				.formattedFor(Locale.US)
				.build();
		reporter = MetricsAggregationReporter.forRegistry(new Metric2Registry()).addOnShutdownReporter(onShutdownReporter).build();
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
		reporter.reportMetrics(map(name("string").build(), gauge("foo")).add(name("double").build(), gauge(2.5d)).add(name("BigInteger").build(), gauge(new BigInteger("2"))),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map());
		reporter.reportMetrics(map(name("string").build(), gauge("bar")).add(name("double").build(), gauge(2.5d * 3)).add(name("BigInteger").build(), gauge(new BigInteger("6"))),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map());

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
		reporter.reportMetrics(MetricsReporterTestHelper.map(),
				map(name("c1").build(), counter(2)),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map());

		reporter.reportMetrics(MetricsReporterTestHelper.map(),
				map(name("counter1").build(), counter(3)).add(name("counter2").build(), counter(5)),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map());

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
		reporter.reportMetrics(MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				map(name("histogram").build(), histogram(1, snapshot(4, 11L, 2L, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0))),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map());

		reporter.reportMetrics(MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				map(name("histogram").build(), histogram(1 * 3, snapshot(4 * 3, 11L * 3, 2L * 3, 5.0 * 3, 6.0 * 3, 7.0 * 3, 8.0 * 3, 9.0 * 3, 10.0 * 3, 11.0 * 3))),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map());

		reporter.onShutDown();

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Histograms ------------------------------------------------------------------\n" +
				"name      | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      |\n" +
				"histogram |         3 |      8.00 |      2.00 |     33.00 |     10.00 |     12.00 |     14.00 |     16.00 |     18.00 |     20.00 |     22.00 | \n\n\n");

	}

	@Test
	public void reportsMeterValues() throws Exception {

		reporter.reportMetrics(MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				map(name("test.meter1").build(), meter(1L)).add(name("test.meter2").build(), meter(2)),
				MetricsReporterTestHelper.map());

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
		reporter.reportMetrics(MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				map(name("timer1").build(), timer(1L, 2.0, 3.0, 4.0, 5.0, snapshot(4, 11L, 2L, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0))));

		reporter.reportMetrics(MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				MetricsReporterTestHelper.map(),
				map(name("timer1").build(), timer(4L, 2.0 * 3, 3.0 * 3, 4.0 * 3, 5.0 * 3, snapshot(4 * 3, 11L * 3, 1, 5.0 * 3, 6.0 * 3, 7.0 * 3, 8.0 * 3, 9.0 * 3, 10.0 * 3, 11.0 * 3))));

		reporter.onShutDown();

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Timers ----------------------------------------------------------------------\n" +
				"name   | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n" +
				"timer1 |         4 |      8.00 |      1.00 |     33.00 |     10.00 |     12.00 |     14.00 |     16.00 |     18.00 |     20.00 |     22.00 |      6.00 |      6.00 |      6.00 |      6.00 | second        | nanoseconds\n\n\n");

	}
}
