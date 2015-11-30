package org.stagemonitor.core.metrics.metrics2;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;

public class ElasticsearchReporter extends ScheduledMetrics2Reporter {

	public static final String STAGEMONITOR_METRICS_INDEX_PREFIX = "stagemonitor-metrics-";
	public static final String METRICS_TYPE = "metrics";
	private final Map<String, String> globalTags;
	private HttpClient httpClient;
	private final Clock clock;
	private final CorePlugin corePlugin;
	private JsonFactory jfactory = new JsonFactory();

	public ElasticsearchReporter(Metric2Registry registry,
								 Metric2Filter filter,
								 TimeUnit rateUnit,
								 TimeUnit durationUnit,
								 Map<String, String> globalTags,
								 HttpClient httpClient,
								 CorePlugin corePlugin) {

		this(registry, filter, rateUnit, durationUnit, globalTags, httpClient, Clock.defaultClock(), corePlugin);
	}

	public ElasticsearchReporter(Metric2Registry registry,
								 Metric2Filter filter,
								 TimeUnit rateUnit,
								 TimeUnit durationUnit,
								 Map<String, String> globalTags,
								 HttpClient httpClient,
								 Clock clock, CorePlugin corePlugin) {

		super(registry, filter, rateUnit, durationUnit);
		this.corePlugin = corePlugin;
		this.globalTags = Collections.unmodifiableMap(new HashMap<String, String>(globalTags));
		this.httpClient = httpClient;
		this.clock = clock;
		jfactory.setCodec(JsonUtils.getMapper());
	}

	@Override
	public void reportMetrics(final Map<MetricName, Gauge> gauges,
							  final Map<MetricName, Counter> counters,
							  final Map<MetricName, Histogram> histograms,
							  final Map<MetricName, Meter> meters,
							  final Map<MetricName, Timer> timers) {
		final Timer.Context time = registry.timer(name("reporting_time").tag("reporter", "elasticsearch").build()).time();
		httpClient.send("POST", corePlugin.getElasticsearchUrl() + "/_bulk", null, new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				String bulkAction = "{ \"index\" : " +
						"{ \"_index\" : \"" + STAGEMONITOR_METRICS_INDEX_PREFIX + StringUtils.getLogstashStyleDate() + "\", " +
						"\"_type\" : \"" + METRICS_TYPE + "\" } " +
						"}\n";
				byte[] bulkActionBytes = bulkAction.getBytes("UTF-8");
				reportMetrics(gauges, counters, histograms, meters, timers, os, bulkActionBytes);
				os.close();
			}
		});
		time.stop();
	}

	public void reportMetrics(Map<MetricName, Gauge> gauges, Map<MetricName, Counter> counters,
							  Map<MetricName, Histogram> histograms, final Map<MetricName, Meter> meters,
							  Map<MetricName, Timer> timers, OutputStream os, byte[] bulkActionBytes) throws IOException {
		long timestamp = clock.getTime();

		reportMetric(gauges, timestamp, new ValueWriter<Gauge>() {
			public void writeValues(Gauge gauge, JsonGenerator jg) throws IOException {
				final Object value = gauge.getValue();
				if (value == null) {
					return;
				}
				if (value instanceof Number) {
					writeDoubleUnlessNaN(jg, "value", ((Number)value).doubleValue());
				} else if (value instanceof Boolean) {
					jg.writeBooleanField("value_boolean", (Boolean) value);
				} else {
					jg.writeStringField("value_string", value.toString());
				}
			}
		}, os, bulkActionBytes);
		reportMetric(counters, timestamp, new ValueWriter<Counter>() {
			public void writeValues(Counter counter, JsonGenerator jg) throws IOException {
				jg.writeObjectField("count", counter.getCount());
			}
		}, os, bulkActionBytes);
		reportMetric(histograms, timestamp, new ValueWriter<Histogram>() {
			public void writeValues(Histogram histogram, JsonGenerator jg) throws IOException {
				final Snapshot snapshot = histogram.getSnapshot();
				jg.writeNumberField("count", histogram.getCount());
				writeSnapshot(snapshot, jg);
			}
		}, os, bulkActionBytes);
		reportMetric(meters, timestamp, new ValueWriter<Meter>() {
			public void writeValues(Meter meter, JsonGenerator jg) throws IOException {
				writeMetered(meter, jg);
			}
		}, os, bulkActionBytes);
		reportMetric(timers, timestamp, new ValueWriter<Timer>() {
			public void writeValues(Timer timer, JsonGenerator jg) throws IOException {
				writeMetered(timer, jg);
				writeSnapshot(timer.getSnapshot(), jg);
			}
		}, os, bulkActionBytes);
	}

	private void writeSnapshot(Snapshot snapshot, JsonGenerator jg) throws IOException {
		writeDoubleUnlessNaN(jg, "min", convertDuration(snapshot.getMin()));
		writeDoubleUnlessNaN(jg, "max", convertDuration(snapshot.getMax()));
		writeDoubleUnlessNaN(jg, "mean", convertDuration(snapshot.getMean()));
		writeDoubleUnlessNaN(jg, "median", convertDuration(snapshot.getMedian()));
		writeDoubleUnlessNaN(jg, "std", convertDuration(snapshot.getStdDev()));
		writeDoubleUnlessNaN(jg, "p25", convertDuration(snapshot.getValue(0.25)));
		writeDoubleUnlessNaN(jg, "p75", convertDuration(snapshot.get75thPercentile()));
		writeDoubleUnlessNaN(jg, "p95", convertDuration(snapshot.get95thPercentile()));
		writeDoubleUnlessNaN(jg, "p98", convertDuration(snapshot.get98thPercentile()));
		writeDoubleUnlessNaN(jg, "p99", convertDuration(snapshot.get99thPercentile()));
		writeDoubleUnlessNaN(jg, "p999", convertDuration(snapshot.get999thPercentile()));
	}

	private void writeMetered(Metered metered, JsonGenerator jg) throws IOException {
		jg.writeNumberField("count", metered.getCount());
		writeDoubleUnlessNaN(jg, "m1_rate", convertRate(metered.getOneMinuteRate()));
		writeDoubleUnlessNaN(jg, "m5_rate", convertRate(metered.getFiveMinuteRate()));
		writeDoubleUnlessNaN(jg, "m15_rate", convertRate(metered.getFifteenMinuteRate()));
		writeDoubleUnlessNaN(jg, "mean_rate", convertRate(metered.getMeanRate()));
	}

	private <T extends Metric> void reportMetric(Map<MetricName, T> metrics, long timestamp, ValueWriter<T> valueWriter,
												 OutputStream os, byte[] bulkActionBytes) throws IOException {

		for (Map.Entry<MetricName, T> entry : metrics.entrySet()) {
			os.write(bulkActionBytes);
			final JsonGenerator jg = jfactory.createGenerator(os);
			jg.writeStartObject();
			MetricName metricName = entry.getKey();
			jg.writeNumberField("@timestamp", timestamp);
			jg.writeStringField("name", metricName.getName());
			writeMap(jg, metricName.getTags());
			writeMap(jg, globalTags);
			valueWriter.writeValues(entry.getValue(), jg);
			jg.writeEndObject();
			jg.flush();
			os.write('\n');
		}
	}

	private static void writeDoubleUnlessNaN(JsonGenerator jg, String key, double value) throws IOException {
		if (!Double.isNaN(value)) {
			jg.writeNumberField(key, value);
		}
	}

	private void writeMap(JsonGenerator jg, Map<String, String> map) throws IOException {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			jg.writeObjectField(entry.getKey(), entry.getValue());
		}
	}

	private interface ValueWriter<T extends Metric> {
		void writeValues(T value, JsonGenerator jg) throws IOException;
	}

}
