package org.stagemonitor.core.metrics.metrics2;

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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
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

public class InfluxDbReporterTest {

	private static final TimeUnit DURATION_UNIT = TimeUnit.MICROSECONDS;

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
		influxDbReporter = InfluxDbReporter.forRegistry(new Metric2Registry(), corePlugin)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(DURATION_UNIT)
				.globalTags(singletonMap("app", "test"))
				.httpClient(httpClient)
				.clock(clock)
				.build();
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
	public void testReportNullGauge() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(name("cpu_usage").type("user").tag("core", "1").build(), gauge(null)),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(Collections.emptyList()));
	}

	@Test
	public void testReportBooleanGauge() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(name("gauge").build(), gauge(true)),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("gauge,app=test value_boolean=true %d", timestamp))));
	}

	@Test
	public void testReportStringGauge() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(name("gauge").build(), gauge("foo")),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("gauge,app=test value_string=\"foo\" %d", timestamp))));
	}

	@Test
	public void testReportGaugesExponent() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(name("cpu_usage").type("user").tag("core", "1").build(), gauge(1e-8)),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("cpu_usage,core=1,type=user,app=test value=1.0e-8 %d", timestamp))));
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
				eq(singletonList(format("web_sessions,app=test count=123i %d", timestamp))));
	}

	@Test
	public void testReportHistograms() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(Counter.class),
				metricNameMap(name("histogram").build(), histogram(400)),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("histogram,app=test count=1i,min=400,max=200,mean=400.0,p50=600.0,std=500.0,p25=0.0,p75=700.0,p95=800.0,p98=900.0,p99=1000.0,p999=1100.0 %d", timestamp))));
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
				eq(singletonList(format("meter,app=test count=10i,m1_rate=3.0,m5_rate=4.0,m15_rate=5.0,mean_rate=2.0 %d", timestamp))));
	}

	@Test
	public void testReportTimers() throws Exception {
		influxDbReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(name("response_time").build(), timer(400)));

		verify(httpClient).send(eq("POST"), eq("http://localhost:8086/write?precision=ms&db=stm"),
				eq(singletonList(format("response_time,app=test count=1i,m1_rate=3.0,m5_rate=4.0,m15_rate=5.0,mean_rate=2.0,min=0.4,max=0.2,mean=0.4,p50=0.6,std=0.5,p25=0.0,p75=0.7,p95=0.8,p98=0.9,p99=1.0,p999=1.1 %d", timestamp))));
	}

	@Test
	public void testGetInfluxDbStringOrderedTags() throws Exception {
		assertEquals("cpu_usage,core=1,level=user",
				InfluxDbReporter.getInfluxDbLineProtocolString(name("cpu_usage").tag("level", "user").tag("core", "1").build()));
	}

	@Test
	public void testGetInfluxDbStringWhiteSpace() throws Exception {
		assertEquals("cpu\\ usage,level=soft\\ irq",
				InfluxDbReporter.getInfluxDbLineProtocolString(name("cpu usage").tag("level", "soft irq").build()));
	}

	@Test
	public void testGetInfluxDbStringNoTags() throws Exception {
		assertEquals("cpu_usage",
				InfluxDbReporter.getInfluxDbLineProtocolString(name("cpu_usage").build()));
	}

	@Test
	public void testGetInfluxDbStringAllEscapingAndQuotingBehavior() throws Exception {
		assertEquals("\"measurement\\ with\\ quotes\",tag\\ key\\ with\\ spaces=tag\\,value\\,with\"commas\"",
				InfluxDbReporter.getInfluxDbLineProtocolString(name("\"measurement with quotes\"").tag("tag key with spaces", "tag,value,with\"commas\"").build()));
	}
}
