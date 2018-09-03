package org.stagemonitor;

import com.codahale.metrics.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.ElasticsearchReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.HttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

@State(value = Scope.Benchmark)
public class ElasticsearchReporterBenchmark {

	private static final TimeUnit DURATION_UNIT = TimeUnit.MICROSECONDS;
	private static final byte[] bulkActionBytes = new byte[] {};

	private ElasticsearchReporter elasticsearchReporter;
	private ByteArrayOutputStream out;
	private CorePlugin corePlugin;
	private Metric2Registry registry;
	private long timestamp;

	private Map<MetricName, Gauge> gauges;
	private Map<MetricName, Counter> counters;
	private Map<MetricName, Histogram> histograms;
	private Map<MetricName, Meter> meters;
	private Map<MetricName, Timer> timers;
	private Counter counter;

	@Setup(Level.Iteration)
	public void init() throws IOException {
		registry = new Metric2Registry();
		for (int g = 0; g < 100; g++) {
			Gauge<Long> gauge = new Gauge<Long>() {
				public Long getValue() {
					return System.currentTimeMillis();
				}
			};
			registry.register(name("test_gauge_" + g).build(), gauge);
			counter = registry.counter(name("test_counter").build());
		}
		gauges = registry.getGauges();
		counters = metricNameMap(Counter.class);
		histograms = metricNameMap(Histogram.class);
		meters = metricNameMap(Meter.class);
		timers = metricNameMap(Timer.class);

		timestamp = System.currentTimeMillis();
		out = new ByteArrayOutputStream();
		HttpClient httpClient = mock(HttpClient.class);
		when(httpClient.send(any(), any(), any(), any(), any())).thenReturn(200);
		corePlugin = mock(CorePlugin.class);
		final ElasticsearchClient elasticsearchClient = mock(ElasticsearchClient.class);
		when(elasticsearchClient.isElasticsearchAvailable()).thenReturn(true);
		when(corePlugin.getElasticsearchClient()).thenReturn(elasticsearchClient);
		when(corePlugin.isStagemonitorActive()).thenReturn(true);
		elasticsearchReporter = ElasticsearchReporter.forRegistry(registry, corePlugin)
			.convertDurationsTo(DURATION_UNIT)
			.globalTags(singletonMap("app", "benchmark"))
			.httpClient(httpClient)
			.build();

	}

	private static <T> Map<MetricName, T> metricNameMap(Class<T> clazz) {
		return Collections.emptyMap();
	}

	@Benchmark
	public void reportMetrics(Blackhole bh) throws IOException {
		counter.inc();
		elasticsearchReporter.reportMetrics(gauges, counters, histograms, meters, timers, out, bulkActionBytes, timestamp);
	}
}
