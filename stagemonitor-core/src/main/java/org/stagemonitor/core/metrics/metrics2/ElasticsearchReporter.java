package org.stagemonitor.core.metrics.metrics2;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ElasticsearchReporter extends ScheduledMetrics2Reporter {

	public static final String STAGEMONITOR_METRICS_INDEX_PREFIX = "stagemonitor-metrics-";
	public static final String ES_METRICS_LOGGER = "ElasticsearchMetrics";
	private static final String METRICS_TYPE = "metrics";
	private static final MetricName reportingTimeMetricName = name("reporting_time").tag("reporter", "elasticsearch").build();

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Logger elasticsearchMetricsLogger;

	private final Map<String, String> globalTags;
	private final CorePlugin corePlugin;
	private final HttpClient httpClient;
	private final JsonFactory jfactory = new JsonFactory();
	private final Metric2RegistryModule metric2RegistryModule;
	private final ElasticsearchClient elasticsearchClient;

	public static ElasticsearchReporter.Builder forRegistry(Metric2Registry registry, CorePlugin corePlugin) {
		return new Builder(registry, corePlugin);
	}

	private ElasticsearchReporter(Builder builder) {
		super(builder);
		this.elasticsearchMetricsLogger = builder.getElasticsearchMetricsLogger();
		this.globalTags = builder.getGlobalTags();
		this.httpClient = builder.getHttpClient();
		this.jfactory.setCodec(JsonUtils.getMapper());
		this.metric2RegistryModule = new Metric2RegistryModule(builder.getRateUnit(), builder.getDurationUnit());
		this.corePlugin = builder.getCorePlugin();
		this.elasticsearchClient = corePlugin.getElasticsearchClient();
	}

	@Override
	public void reportMetrics(final Map<MetricName, Gauge> gauges,
							  final Map<MetricName, Counter> counters,
							  final Map<MetricName, Histogram> histograms,
							  final Map<MetricName, Meter> meters,
							  final Map<MetricName, Timer> timers) {

		long timestamp = clock.getTime();

		final Timer.Context time = registry.timer(reportingTimeMetricName).time();
		final MetricsOutputStreamHandler metricsOutputStreamHandler = new MetricsOutputStreamHandler(gauges, counters, histograms, meters, timers, timestamp);
		if (!corePlugin.isOnlyLogElasticsearchMetricReports()) {
			if (!elasticsearchClient.isElasticsearchAvailable()) {
				return;
			}

			httpClient.send("POST", corePlugin.getElasticsearchUrl() + "/_bulk", null,
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
							  Map<MetricName, Timer> timers, OutputStream os, byte[] bulkActionBytes, long timestamp) throws IOException {

		reportMetric(gauges, timestamp, metric2RegistryModule.getValueWriter(Gauge.class), os, bulkActionBytes);
		reportMetric(counters, timestamp, metric2RegistryModule.getValueWriter(Counter.class), os, bulkActionBytes);
		reportMetric(histograms, timestamp, metric2RegistryModule.getValueWriter(Histogram.class), os, bulkActionBytes);
		reportMetric(meters, timestamp, metric2RegistryModule.getValueWriter(Meter.class), os, bulkActionBytes);
		reportMetric(timers, timestamp, metric2RegistryModule.getValueWriter(Timer.class), os, bulkActionBytes);
	}

	private <T extends Metric> void reportMetric(Map<MetricName, T> metrics, long timestamp, Metric2RegistryModule.ValueWriter<T> valueWriter,
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

	private void writeMap(JsonGenerator jg, Map<String, String> map) throws IOException {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			jg.writeObjectField(entry.getKey(), entry.getValue());
		}
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
			String bulkAction = ElasticsearchClient.getBulkHeader("index", STAGEMONITOR_METRICS_INDEX_PREFIX + StringUtils.getLogstashStyleDate(), METRICS_TYPE);
			byte[] bulkActionBytes = bulkAction.getBytes("UTF-8");
			reportMetrics(gauges, counters, histograms, meters, timers, os, bulkActionBytes, timestamp);
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
