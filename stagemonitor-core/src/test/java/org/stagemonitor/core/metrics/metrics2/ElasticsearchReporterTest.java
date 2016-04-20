package org.stagemonitor.core.metrics.metrics2;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.counter;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.gauge;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.histogram;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.map;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.meter;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.metricNameMap;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.objectMap;
import static org.stagemonitor.core.metrics.MetricsReporterTestHelper.timer;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.MetricNameFilter;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;

public class ElasticsearchReporterTest {

	private ElasticsearchReporter elasticsearchReporter;
	private long timestamp;
	private ByteArrayOutputStream out;
	private Logger metricsLogger;
	private CorePlugin corePlugin;
	private Metric2Registry registry;
	private Clock clock;

	@Before
	public void setUp() throws Exception {
		this.clock = mock(Clock.class);
		timestamp = System.currentTimeMillis();
		when(clock.getTime()).thenReturn(timestamp);
		final HttpClient httpClient = mock(HttpClient.class);
		when(httpClient.send(any(), any(), any(), any())).thenAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				HttpClient.OutputStreamHandler handler = (HttpClient.OutputStreamHandler) invocation.getArguments()[3];
				final HttpURLConnection connection = mock(HttpURLConnection.class);
				when(connection.getOutputStream()).thenReturn(out);
				handler.withHttpURLConnection(connection.getOutputStream());
				return 200;
			}
		});
		metricsLogger = mock(Logger.class);
		corePlugin = mock(CorePlugin.class);
		registry = new Metric2Registry();
		elasticsearchReporter = ElasticsearchReporter.forRegistry(registry, corePlugin)
				.convertDurationsTo(TimeUnit.NANOSECONDS)
				.globalTags(singletonMap("app", "test"))
				.httpClient(httpClient)
				.clock(clock)
				.elasticsearchMetricsLogger(metricsLogger)
				.filter(MetricNameFilter.excludePatterns(singleton(name("reporting_time").build())))
				.build();

		out = new ByteArrayOutputStream();
	}

	@After
	public void tearDown() throws Exception {
		elasticsearchReporter.close();
	}

	@Test(expected = IllegalStateException.class)
	public void testScheduleTwice() throws Exception {
		elasticsearchReporter.start(100, TimeUnit.MILLISECONDS);
		elasticsearchReporter.start(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testSchedule() throws Exception {
		when(corePlugin.isOnlyLogElasticsearchMetricReports()).thenReturn(true);
		// this clock starts at 0 and then progresses normally
		when(clock.getTime()).then(new Answer<Long>() {
			long firstTimestamp;

			@Override
			public Long answer(InvocationOnMock invocation) throws Throwable {
				final long time = System.currentTimeMillis();
				if (firstTimestamp == 0) {
					firstTimestamp = time;
				}
				return (time - firstTimestamp);
			}
		});

		registry.register(name("test").build(), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return 1;
			}
		});

		elasticsearchReporter.start(100, TimeUnit.MILLISECONDS);
		Thread.sleep(300);
		verify(metricsLogger).info(eq(String.format("{\"index\":{\"_index\":\"stagemonitor-metrics-%s\",\"_type\":\"metrics\"}}\n" +
				"{\"@timestamp\":100,\"name\":\"test\",\"app\":\"test\",\"value\":1.0}\n", StringUtils.getLogstashStyleDate())));
		verify(metricsLogger).info(eq(String.format("{\"index\":{\"_index\":\"stagemonitor-metrics-%s\",\"_type\":\"metrics\"}}\n" +
				"{\"@timestamp\":200,\"name\":\"test\",\"app\":\"test\",\"value\":1.0}\n", StringUtils.getLogstashStyleDate())));
	}

	@Test
	public void testReportGauges() throws Exception {
		elasticsearchReporter.reportMetrics(
				metricNameMap(
						name("cpu_usage").type("user").tag("core", "1").build(), gauge(3)
				),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		final String jsons = new String(out.toByteArray());
		assertEquals(jsons, 2, jsons.split("\n").length);

		assertEquals(
				objectMap("index", map("_type", "metrics")
						.add("_index", "stagemonitor-metrics-" + StringUtils.getLogstashStyleDate())),
				asMap(jsons.split("\n")[0]));

		assertEquals(
				objectMap("@timestamp", timestamp)
					.add("name", "cpu_usage")
					.add("app", "test")
					.add("type", "user")
					.add("core", "1")
					.add("value", 3.0),
				asMap(jsons.split("\n")[1]));
	}

	@Test
	public void testReportNullGauge() throws Exception {
		elasticsearchReporter.reportMetrics(
				metricNameMap(name("gauge").build(), gauge(null)),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		assertEquals(
				objectMap("@timestamp", timestamp)
						.add("name", "gauge")
						.add("app", "test"),
				asMap(out));
	}

	@Test
	public void testReportToLog() throws Exception {
		when(corePlugin.isOnlyLogElasticsearchMetricReports()).thenReturn(true);

		elasticsearchReporter.reportMetrics(
				metricNameMap(name("gauge").build(), gauge(1)),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		verify(metricsLogger).info(eq(String.format("{\"index\":{\"_index\":\"stagemonitor-metrics-%s\",\"_type\":\"metrics\"}}\n" +
				"{\"@timestamp\":%d,\"name\":\"gauge\",\"app\":\"test\",\"value\":1.0}\n", StringUtils.getLogstashStyleDate(), timestamp)));
	}

	@Test
	public void testReportBooleanGauge() throws Exception {
		elasticsearchReporter.reportMetrics(
				metricNameMap(name("gauge").build(), gauge(true)),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		assertEquals(
				objectMap("@timestamp", timestamp)
						.add("name", "gauge")
						.add("app", "test")
						.add("value_boolean", true),
				asMap(out));
	}

	@Test
	public void testReportStringGauge() throws Exception {
		elasticsearchReporter.reportMetrics(
				metricNameMap(name("gauge").build(), gauge("foo")),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		assertEquals(
				objectMap("@timestamp", timestamp)
						.add("name", "gauge")
						.add("app", "test")
						.add("value_string", "foo"),
				asMap(out));
	}

	@Test
	public void testReportCounters() throws Exception {
		elasticsearchReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(name("web_sessions").build(), counter(123)),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		assertEquals(
				map("@timestamp", timestamp, Object.class)
						.add("name", "web_sessions")
						.add("app", "test")
						.add("count", 123),
				asMap(out));
	}

	@Test
	public void testReportHistograms() throws Exception {
		elasticsearchReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(Counter.class),
				metricNameMap(name("histogram").build(), histogram(4)),
				metricNameMap(Meter.class),
				metricNameMap(Timer.class));

		assertEquals(objectMap("@timestamp", timestamp)
						.add("name", "histogram")
						.add("app", "test")
						.add("count", 1)
						.add("max", 2.0)
						.add("mean", 4.0)
						.add("median", 6.0)
						.add("min", 4.0)
						.add("p25", 0.0)
						.add("p75", 7.0)
						.add("p95", 8.0)
						.add("p98", 9.0)
						.add("p99", 10.0)
						.add("p999", 11.0)
						.add("std", 5.0),
				asMap(out));
	}

	@Test
	public void testReportMeters() throws Exception {
		elasticsearchReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(name("meter").build(), meter(10)),
				metricNameMap(Timer.class));

		assertEquals(map("@timestamp", timestamp, Object.class)
						.add("name", "meter")
						.add("app", "test")
						.add("count", 10)
						.add("m15_rate", 5.0)
						.add("m1_rate", 3.0)
						.add("m5_rate", 4.0)
						.add("mean_rate", 2.0),
				asMap(out));
	}

	@Test
	public void testReportTimers() throws Exception {
		elasticsearchReporter.reportMetrics(
				metricNameMap(Gauge.class),
				metricNameMap(Counter.class),
				metricNameMap(Histogram.class),
				metricNameMap(Meter.class),
				metricNameMap(name("response_time").build(), timer(4)));

		assertEquals(map("@timestamp", timestamp, Object.class)
						.add("name", "response_time")
						.add("app", "test")
						.add("count", 1)
						.add("m15_rate", 5.0)
						.add("m1_rate", 3.0)
						.add("m5_rate", 4.0)
						.add("mean_rate", 2.0)
						.add("max", 2.0)
						.add("mean", 4.0)
						.add("median", 6.0)
						.add("min", 4.0)
						.add("p25", 0.0)
						.add("p75", 7.0)
						.add("p95", 8.0)
						.add("p98", 9.0)
						.add("p99", 10.0)
						.add("p999", 11.0)
						.add("std", 5.0),
				asMap(out));
	}

	private Map<String, Object> asMap(ByteArrayOutputStream os) throws java.io.IOException {
		return asMap(new String(os.toByteArray()).split("\n")[1]);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(String json) throws java.io.IOException {
		final TreeMap<String, Object> result = new TreeMap<String, Object>();
		result.putAll(JsonUtils.getMapper().readValue(json, Map.class));
		return result;
	}
}