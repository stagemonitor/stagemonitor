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

	private Logger logger;
	private SortedTableLogReporter reporter;

	@Before
	public void setUp() throws Exception {
		logger = mock(Logger.class);
		reporter = SortedTableLogReporter
				.forRegistry(mock(Metric2Registry.class))
				.log(logger)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.NANOSECONDS)
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
				"test.histogram2 |         1 |      4.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 | \n" +
				" test.histogram |         1 |      3.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 | \n" +
				"\n" +
				"\n");
	}

	private FluentMap<MetricName, Histogram> testHistograms() {
		return map(name("test.histogram").build(), histogram(3.0)).add(name("test.histogram2").build(), histogram(4.0));
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
				"        test.meter2 |         2 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"test.meter1,foo=bar |         1 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"\n" +
				"\n");
	}

	@Test
	public void reportsTimerValues() throws Exception {

		reporter.reportMetrics(map(),
				map(),
				map(),
				map(),
				map(name("timer1").build(), timer(4)).add(name("timer2").build(), timer(2)).add(name("timer3").build(), timer(3)));

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Timers ----------------------------------------------------------------------\n" +
				"name   | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n" +
				"timer1 |         1 |      4.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"timer3 |         1 |      3.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"timer2 |         1 |      2.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"\n" +
				"\n");
	}

}
