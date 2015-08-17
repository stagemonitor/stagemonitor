package org.stagemonitor.core.metrics.metrics2;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.counter;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.gauge;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.histogram;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.meter;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.metricNameMap;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.timer;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.HttpClient;

public class InfluxDbReporterTest {

	private InfluxDbReporter influxDbReporter;
	private HttpClient httpClient;
	private long timestamp;

	@Before
	public void setUp() throws Exception {
		httpClient = mock(HttpClient.class);
		Clock clock = mock(Clock.class);
		timestamp = System.currentTimeMillis();
		when(clock.getTime()).thenReturn(timestamp);
		final CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getInfluxDbUrl()).thenReturn("http://localhost:8086");
		when(corePlugin.getInfluxDbDb()).thenReturn("stm");
		influxDbReporter = new InfluxDbReporter(null, null, TimeUnit.SECONDS, TimeUnit.NANOSECONDS, singletonMap("app", "test"), httpClient, clock, corePlugin);
	}

	@Test
	public void testReportGauges() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(name("cpu_usage").type("user").tag("core", "1").build(), gauge(3)),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("cpu_usage,core=1,type=user,app=test value=3 %d", timestamp))));
	}

	@Test
	public void testReportCounters() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(name("web_sessions").build(), counter(123)),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("web_sessions,app=test count=123 %d", timestamp))));
	}

	@Test
	public void testReportHistograms() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(Counter.class),
				metricNameMap(name("histogram").build(), histogram(4)),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("histogram,app=test count=1,min=4.0,max=2.0,mean=4.0,median=6.0,std=5.0,p25=0.0,p75=7.0,p95=8.0,p98=9.0,p99=10.0,p999=11.0 %d", timestamp))));
	}

	@Test
	public void testReportMeters() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(name("meter").build(), meter(10)),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("meter,app=test count=10,m1_rate=3.0,m5_rate=4.0,m15_rate=5.0,mean_rate=2.0 %d", timestamp))));
	}

	@Test
	public void testReportTimers() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(name("response_time").build(), timer(4)));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("response_time,app=test count=1,m1_rate=3.0,m5_rate=4.0,m15_rate=5.0,mean_rate=2.0,min=4.0,max=2.0,mean=4.0,median=6.0,std=5.0,p25=0.0,p75=7.0,p95=8.0,p98=9.0,p99=10.0,p999=11.0 %d", timestamp))));
	}
}
