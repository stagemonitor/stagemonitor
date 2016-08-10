package org.stagemonitor.core.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class SortedTableLogReporterTest extends MetricsReporterTestHelper {

	private static final TimeUnit DURATION_UNIT = TimeUnit.MICROSECONDS;
	private static final double DURATION_FACTOR = 1.0 / DURATION_UNIT.toNanos(1);
	
	private Logger logger;
	private SortedTableLogReporter reporter;

	@Before
	public void setUp() throws Exception {
		logger = mock(Logger.class);
		reporter = SortedTableLogReporter
				.forRegistry(mock(Metric2Registry.class))
				.log(logger)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(DURATION_UNIT)
				.formattedFor(Locale.US)
				.build();
	}

	@Test
	public void reportsGaugeValues() throws Exception {
		reporter.reportMetrics(testGauges(), map(), map(), map(), map());

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

	private FluentMap<MetricName, Gauge> testGauges() {
		return map(name("gauge3").build(), gauge(3)).add(name("gaugeWithLongName1").build(), gauge(1)).add(name("gauge2").build(), gauge(2));
	}

	@Test
	public void reportsCounterValues() throws Exception {
		reporter.reportMetrics(map(), map(name("test.counter").build(), counter(100L)).add(name("test.counter2").build(), counter(200L)),
				map(), map(), map());

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

		reporter.reportMetrics(map(),
				map(),
				testHistograms(),
				map(),
				map());

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Histograms ------------------------------------------------------------------\n" +
				"name            | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      |\n" +
				"test.histogram2 |         1 |    400.00 |    400.00 |    200.00 |    500.00 |    600.00 |    700.00 |    800.00 |    900.00 |  1,000.00 |  1,100.00 | \n" +
				" test.histogram |         1 |    300.00 |    400.00 |    200.00 |    500.00 |    600.00 |    700.00 |    800.00 |    900.00 |  1,000.00 |  1,100.00 | \n" +
				"\n" +
				"\n");
	}

	private FluentMap<MetricName, Histogram> testHistograms() {
		return map(name("test.histogram").build(), histogram(300.0)).add(name("test.histogram2").build(), histogram(400.0));
	}

	@Test
	public void reportsMeterValues() throws Exception {

		reporter.reportMetrics(map(),
				map(),
				map(),
				map(name("test.meter1").tag("foo", "bar").build(), meter(1L)).add(name("test.meter2").build(), meter(2)),
				map());

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Meters ----------------------------------------------------------------------\n" +
				"name                | count     | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n" +
				"        test.meter2 |         2 |      2.00 |      3.00 |      4.00 |      5.00 | second        | microseconds\n" +
				"test.meter1,foo=bar |         1 |      2.00 |      3.00 |      4.00 |      5.00 | second        | microseconds\n" +
				"\n" +
				"\n");
	}

	@Test
	public void reportsTimerValues() throws Exception {

		reporter.reportMetrics(map(),
				map(),
				map(),
				map(),
				map(name("timer1").build(), timer(400)).add(name("timer2").build(), timer(200)).add(name("timer3").build(), timer(300)));

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Timers ----------------------------------------------------------------------\n" +
				"name   | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n" +
				"timer1 |         1 |      0.40 |      0.40 |      0.20 |      0.50 |      0.60 |      0.70 |      0.80 |      0.90 |      1.00 |      1.10 |      2.00 |      3.00 |      4.00 |      5.00 | second        | microseconds\n" +
				"timer3 |         1 |      0.30 |      0.40 |      0.20 |      0.50 |      0.60 |      0.70 |      0.80 |      0.90 |      1.00 |      1.10 |      2.00 |      3.00 |      4.00 |      5.00 | second        | microseconds\n" +
				"timer2 |         1 |      0.20 |      0.40 |      0.20 |      0.50 |      0.60 |      0.70 |      0.80 |      0.90 |      1.00 |      1.10 |      2.00 |      3.00 |      4.00 |      5.00 | second        | microseconds\n" +
				"\n" +
				"\n");
	}

}
