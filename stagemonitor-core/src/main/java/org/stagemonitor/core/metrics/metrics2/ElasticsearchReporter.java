package org.stagemonitor.core.metrics.metrics2;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;

public class ElasticsearchReporter extends ScheduledMetrics2Reporter {

	public static final String STAGEMONITOR_METRICS_INDEX_PREFIX = "stagemonitor-metrics-";
	public static final String ES_METRICS_LOGGER = "ElasticsearchMetrics";
	private static final String METRICS_TYPE = "metrics";
	private static final byte[] BULK_INDEX_HEADER = "{\"index\":{}}\n".getBytes(Charset.forName("UTF-8"));

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Logger elasticsearchMetricsLogger;

	private final Map<String, String> globalTags;
	private final CorePlugin corePlugin;
	private final HttpClient httpClient;
	private final JsonFactory jfactory = new JsonFactory();

	public static ElasticsearchReporter.Builder forRegistry(Metric2Registry registry, CorePlugin corePlugin) {
		return new Builder(registry, corePlugin);
	}

	private ElasticsearchReporter(Builder builder) {
		super(builder);
		this.elasticsearchMetricsLogger = builder.getElasticsearchMetricsLogger();
		this.globalTags = builder.getGlobalTags();
		this.httpClient = builder.getHttpClient();
		this.jfactory.setCodec(JsonUtils.getMapper());
		this.corePlugin = builder.getCorePlugin();
	}

	@Override
	public void reportMetrics(final Map<MetricName, Gauge> gauges,
							  final Map<MetricName, Counter> counters,
							  final Map<MetricName, Histogram> histograms,
							  final Map<MetricName, Meter> meters,
							  final Map<MetricName, Timer> timers) {
		long timestamp = clock.getTime();

		final Timer.Context time = registry.timer(name("reporting_time").tag("reporter", "elasticsearch").build()).time();
		final MetricsOutputStreamHandler metricsOutputStreamHandler = new MetricsOutputStreamHandler(gauges, counters, histograms, meters, timers, timestamp);
		if (!corePlugin.isOnlyLogElasticsearchMetricReports()) {
			final String path = "/" + STAGEMONITOR_METRICS_INDEX_PREFIX + StringUtils.getLogstashStyleDate() + "/" + METRICS_TYPE + "/_bulk";
			httpClient.send("POST", corePlugin.getElasticsearchUrl() + path, null,
					metricsOutputStreamHandler, new ElasticsearchClient.BulkErrorReportingResponseHandler());
		} else {
			try {
				final ByteArrayOutputStream os = new ByteArrayOutputStream();
				metricsOutputStreamHandler.withHttpURLConnection(os);
				elasticsearchMetricsLogger.info(os.toString("UTF-8"));
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		time.stop();
	}

	public void reportMetrics(Map<MetricName, Gauge> gauges, Map<MetricName, Counter> counters,
							  Map<MetricName, Histogram> histograms, final Map<MetricName, Meter> meters,
							  Map<MetricName, Timer> timers, OutputStream os, long timestamp) throws IOException {

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
		}, os);
		reportMetric(counters, timestamp, new ValueWriter<Counter>() {
			public void writeValues(Counter counter, JsonGenerator jg) throws IOException {
				jg.writeObjectField("count", counter.getCount());
			}
		}, os);
		reportMetric(histograms, timestamp, new ValueWriter<Histogram>() {
			public void writeValues(Histogram histogram, JsonGenerator jg) throws IOException {
				final Snapshot snapshot = histogram.getSnapshot();
				jg.writeNumberField("count", histogram.getCount());
				writeSnapshot(snapshot, jg);
			}
		}, os);
		reportMetric(meters, timestamp, new ValueWriter<Meter>() {
			public void writeValues(Meter meter, JsonGenerator jg) throws IOException {
				writeMetered(meter, jg);
			}
		}, os);
		reportMetric(timers, timestamp, new ValueWriter<Timer>() {
			public void writeValues(Timer timer, JsonGenerator jg) throws IOException {
				writeMetered(timer, jg);
				writeSnapshot(timer.getSnapshot(), jg);
			}
		}, os);
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
												 OutputStream os) throws IOException {

		for (Map.Entry<MetricName, T> entry : metrics.entrySet()) {
			os.write(BULK_INDEX_HEADER);
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

	private class MetricsOutputStreamHandler implements HttpClient.OutputStreamHandler {
		private final Map<MetricName, Gauge> gauges;
		private final Map<MetricName, Counter> counters;
		private final Map<MetricName, Histogram> histograms;
		private final Map<MetricName, Meter> meters;
		private final Map<MetricName, Timer> timers;
		private final long timestamp;

		public MetricsOutputStreamHandler(Map<MetricName, Gauge> gauges, Map<MetricName, Counter> counters, Map<MetricName, Histogram> histograms, Map<MetricName, Meter> meters, Map<MetricName, Timer> timers, long timestamp) {
			this.gauges = gauges;
			this.counters = counters;
			this.histograms = histograms;
			this.meters = meters;
			this.timers = timers;
			this.timestamp = timestamp;
		}

		@Override
		public void withHttpURLConnection(OutputStream os) throws IOException {
			reportMetrics(gauges, counters, histograms, meters, timers, os, timestamp);
			os.close();
		}
	}

	public static class Builder extends ScheduledMetrics2Reporter.Builder<ElasticsearchReporter, Builder> {
		private HttpClient httpClient = new HttpClient();
		private Logger elasticsearchMetricsLogger = LoggerFactory.getLogger(ES_METRICS_LOGGER);
		private final CorePlugin corePlugin;

		private Builder(Metric2Registry registry, CorePlugin corePlugin) {
			super(registry, "stagemonitor-elasticsearch-reporter");
			this.corePlugin = corePlugin;
		}

		@Override
		public ElasticsearchReporter build() {
			return new ElasticsearchReporter(this);
		}

		public HttpClient getHttpClient() {
			return httpClient;
		}


		public Logger getElasticsearchMetricsLogger() {
			return elasticsearchMetricsLogger;
		}

		public Builder httpClient(HttpClient httpClient) {
			this.httpClient = httpClient;
			return this;
		}

		public Builder elasticsearchMetricsLogger(Logger elasticsearchMetricsLogger) {
			this.elasticsearchMetricsLogger = elasticsearchMetricsLogger;
			return this;
		}

		public CorePlugin getCorePlugin() {
			return corePlugin;
		}

	}
}
