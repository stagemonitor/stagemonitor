package org.stagemonitor.core.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SortedTableLogReporterTest extends MetricsReporterTestHelper {

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
				.formattedFor(Locale.US)
				.build();
	}

	@Test
	public void reportsGaugeValues() throws Exception {
		reporter.report(testGauges(), this.<Counter>map(), this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

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

	private FluentMap<String, Gauge> testGauges() {
		return map("gauge3", gauge(3)).add("gaugeWithLongName1", gauge(1)).add("gauge2", gauge(2));
	}

	@Test
	public void reportsCounterValues() throws Exception {
		reporter.report(this.<Gauge>map(), map("test.counter", counter(100L)).add("test.counter2", counter(200L)),
				this.<Histogram>map(), this.<Meter>map(), this.<Timer>map());

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
				testHistograms(),
				this.<Meter>map(),
				this.<Timer>map());

		verify(logger).info("Metrics ========================================================================\n" +
				"\n" +
				"-- Histograms ------------------------------------------------------------------\n" +
				"name            | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      |\n" +
				"test.histogram2 |         1 |      4.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 | \n" +
				" test.histogram |         1 |      3.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 | \n" +
				"\n" +
				"\n");
	}

	private FluentMap<String, Histogram> testHistograms() {
		return map("test.histogram", histogram(3.0)).add("test.histogram2", histogram(4.0));
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
				"test.meter2 |         2 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"test.meter1 |         1 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"\n" +
				"\n");
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
				"timer1 |         1 |      4.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"timer3 |         1 |      3.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"timer2 |         1 |      2.00 |      4.00 |      2.00 |      5.00 |      6.00 |      7.00 |      8.00 |      9.00 |     10.00 |     11.00 |      2.00 |      3.00 |      4.00 |      5.00 | second        | nanoseconds\n" +
				"\n" +
				"\n");
	}

}
