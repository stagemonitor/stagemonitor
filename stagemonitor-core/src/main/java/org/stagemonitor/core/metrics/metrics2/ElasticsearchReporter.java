package org.stagemonitor.core.metrics.metrics2;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
		this.globalTags = globalTags;
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
		httpClient.send("POST", corePlugin.getElasticsearchUrl() + "/_bulk", null, new HttpClient.HttpURLConnectionHandler() {
			@Override
			public void withHttpURLConnection(HttpURLConnection connection) throws IOException {
				String bulkAction = "{ \"index\" : " +
						"{ \"_index\" : \"stagemonitor-metrics-" + StringUtils.getLogstashStyleDate() + "\", " +
						"\"_type\" : \"metrics\" } " +
						"}\n";
				byte[] bulkActionBytes = bulkAction.getBytes("UTF-8");
				final OutputStream os = connection.getOutputStream();
				reportMetrics(gauges, counters, histograms, meters, timers, os, bulkActionBytes);
				os.close();
			}
		});
		time.stop();
	}

	public void reportMetrics(Map<MetricName, Gauge> gauges, Map<MetricName, Counter> counters, Map<MetricName, Histogram> histograms, final Map<MetricName, Meter> meters, Map<MetricName, Timer> timers, OutputStream os, byte[] bulkActionBytes) throws IOException {
		long timestamp = clock.getTime();

		reportMetric(gauges, timestamp, new ValueWriter<Gauge>() {
			public void writeValues(Gauge gauge, JsonGenerator jg) throws IOException {
				final Object value = gauge.getValue();
				if (value == null) {
					return;
				}
				if (value instanceof Number) {
					jg.writeNumberField("value", ((Number)value).doubleValue());
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
		jg.writeNumberField("min", convertDuration(snapshot.getMin()));
		jg.writeNumberField("max", convertDuration(snapshot.getMax()));
		jg.writeNumberField("mean", convertDuration(snapshot.getMean()));
		jg.writeNumberField("median", convertDuration(snapshot.getMedian()));
		jg.writeNumberField("std", convertDuration(snapshot.getStdDev()));
		jg.writeNumberField("p25", convertDuration(snapshot.getValue(0.25)));
		jg.writeNumberField("p75", convertDuration(snapshot.get75thPercentile()));
		jg.writeNumberField("p95", convertDuration(snapshot.get95thPercentile()));
		jg.writeNumberField("p98", convertDuration(snapshot.get98thPercentile()));
		jg.writeNumberField("p99", convertDuration(snapshot.get99thPercentile()));
		jg.writeNumberField("p999", convertDuration(snapshot.get999thPercentile()));
	}

	private void writeMetered(Metered metered, JsonGenerator jg) throws IOException {
		jg.writeNumberField("count", metered.getCount());
		jg.writeNumberField("m1_rate", convertRate(metered.getOneMinuteRate()));
		jg.writeNumberField("m5_rate", convertRate(metered.getFiveMinuteRate()));
		jg.writeNumberField("m15_rate", convertRate(metered.getFifteenMinuteRate()));
		jg.writeNumberField("mean_rate", convertRate(metered.getMeanRate()));
	}

	private <T extends Metric> void reportMetric(Map<MetricName, T> metrics, long timestamp, ValueWriter<T> valueWriter, OutputStream os, byte[] bulkActionBytes) throws IOException {
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

	private void writeMap(JsonGenerator jg, Map<String, String> map) throws IOException {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			jg.writeObjectField(entry.getKey(), entry.getValue());
		}
	}

	private interface ValueWriter<T extends Metric> {
		void writeValues(T value, JsonGenerator jg) throws IOException;
	}

}
