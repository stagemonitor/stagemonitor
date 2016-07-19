package org.stagemonitor.core.metrics.metrics2;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.HttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class InfluxDbReporter extends ScheduledMetrics2Reporter {

	private static final int MAX_BATCH_SIZE = 5000;
	private static final Map<MetricName, String> metricNameToInfluxDBFormatCache = new ConcurrentHashMap<MetricName, String>();
	private static final MetricName reportingTimeMetricName = name("reporting_time").tag("reporter", "influxdb").build();

	private List<String> batchLines = new ArrayList<String>(MAX_BATCH_SIZE);
	private final String globalTags;
	private HttpClient httpClient;
	private final CorePlugin corePlugin;

	public static Builder forRegistry(Metric2Registry registry, CorePlugin corePlugin) {
		return new Builder(registry, corePlugin);
	}

	private InfluxDbReporter(Builder builder) {
		super(builder);
		this.globalTags = getInfluxDbTags(builder.getGlobalTags());
		this.httpClient = builder.getHttpClient();
		this.corePlugin = builder.getCorePlugin();
	}

	@Override
	public void reportMetrics(Map<MetricName, Gauge> gauges,
							  Map<MetricName, Counter> counters,
							  Map<MetricName, Histogram> histograms,
							  Map<MetricName, Meter> meters,
							  Map<MetricName, Timer> timers) {

		final Timer.Context time = registry.timer(reportingTimeMetricName).time();
		long timestamp = clock.getTime();
		reportGauges(gauges, timestamp);
		reportCounter(counters, timestamp);
		reportHistograms(histograms, timestamp);
		reportMeters(meters, timestamp);
		reportTimers(timers, timestamp);
		flush();
		time.stop();
	}

	private void reportGauges(Map<MetricName, Gauge> gauges, long timestamp) {
		for (Map.Entry<MetricName, Gauge> entry : gauges.entrySet()) {
			final String value = getGaugeValueForInfluxDb(entry.getValue().getValue());
			if (value != null) {
				reportLine(getInfluxDbLineProtocolString(entry.getKey()), value, timestamp);
			}
		}
	}

	private void reportCounter(Map<MetricName, Counter> counters, long timestamp) {
		for (Map.Entry<MetricName, Counter> entry : counters.entrySet()) {
			final Counter counter = entry.getValue();
			reportLine(getInfluxDbLineProtocolString(entry.getKey()),
					"count=" + getIntegerValue(counter.getCount()), timestamp);
		}
	}

	private void reportHistograms(Map<MetricName, Histogram> histograms, long timestamp) {
		for (Map.Entry<MetricName, Histogram> entry : histograms.entrySet()) {
			final Histogram hist = entry.getValue();
			final Snapshot snapshot = hist.getSnapshot();
			reportLine(getInfluxDbLineProtocolString(entry.getKey()),
					"count=" + getIntegerValue(hist.getCount()) + ","
							+ reportSnapshot(snapshot), timestamp);
		}
	}

	private void reportMeters(Map<MetricName, Meter> meters, long timestamp) {
		for (Map.Entry<MetricName, Meter> entry : meters.entrySet()) {
			final Meter meter = entry.getValue();
			reportLine(getInfluxDbLineProtocolString(entry.getKey()), reportMetered(meter), timestamp);
		}
	}

	private void reportTimers(Map<MetricName, Timer> timers, long timestamp) {
		for (Map.Entry<MetricName, Timer> entry : timers.entrySet()) {
			final Timer timer = entry.getValue();
			final Snapshot snapshot = timer.getSnapshot();
			reportLine(getInfluxDbLineProtocolString(entry.getKey()),
					reportMetered(timer) + ","
							+ reportSnapshot(snapshot), timestamp);
		}
	}

	private String reportSnapshot(Snapshot snapshot) {
		return "min=" + getDuration(snapshot.getMin()) + ","
				+ "max=" + getDuration(snapshot.getMax()) + ","
				+ "mean=" + getDuration(snapshot.getMean()) + ","
				+ "p50=" + getDuration(snapshot.getMedian()) + ","
				+ "std=" + getDuration(snapshot.getStdDev()) + ","
				+ "p25=" + getDuration(snapshot.getValue(0.25)) + ","
				+ "p75=" + getDuration(snapshot.get75thPercentile()) + ","
				+ "p95=" + getDuration(snapshot.get95thPercentile()) + ","
				+ "p98=" + getDuration(snapshot.get98thPercentile()) + ","
				+ "p99=" + getDuration(snapshot.get99thPercentile()) + ","
				+ "p999=" + getDuration(snapshot.get999thPercentile());
	}

	private String reportMetered(Metered metered) {
		return "count=" + getIntegerValue(metered.getCount()) + ","
				+ "m1_rate=" + getRate(metered.getOneMinuteRate()) + ","
				+ "m5_rate=" + getRate(metered.getFiveMinuteRate()) + ","
				+ "m15_rate=" + getRate(metered.getFifteenMinuteRate()) + ","
				+ "mean_rate=" + getRate(metered.getMeanRate());
	}

	private void reportLine(String nameAndTags, String fields, long timestamp) {
		if (batchLines.size() >= MAX_BATCH_SIZE) {
			flush();
		}
		batchLines.add(nameAndTags + globalTags + ' ' + fields + ' ' + timestamp);
	}

	private void flush() {
		httpClient.send("POST", corePlugin.getInfluxDbUrl() + "/write?precision=ms&db=" + corePlugin.getInfluxDbDb(), batchLines);
		batchLines = new ArrayList<String>(MAX_BATCH_SIZE);
	}

	private String getGaugeValueForInfluxDb(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			final String floatValue = getFloatValue(value);
			if (floatValue == null) {
				return null;
			}
			return "value=" + floatValue;
		} else if (value instanceof Boolean) {
			return "value_boolean=" + value.toString();
		} else {
			return "value_string=" + getStringValue(String.valueOf(value));
		}
	}

	private String getIntegerValue(Object integer) {
		return integer.toString() + "i";
	}

	private String getDuration(double duration) {
		return getFloatValue(convertDuration(duration));
	}

	private String getRate(double rate) {
		return getFloatValue(convertRate(rate));
	}

	private String getFloatValue(Object number) {
		String result = number.toString();
		if (result.equals("NaN") || result.contains("Infinity")) {
			return null;
		} else {
			// InfluxDB wants the exponent to be in lower case
			return result.replace('E', 'e');
		}
	}

	private String getStringValue(String value) {
		final String s = value;
		if (s.indexOf('"') == -1) {
			return new StringBuilder(s.length() + 2).append('"').append(s).append('"').toString();
		} else {
			return new StringBuilder(s.length() + 6).append('"').append(s.replace("\"", "\\\"")).append('"').toString();
		}
	}

	public static class Builder extends ScheduledMetrics2Reporter.Builder<InfluxDbReporter, Builder> {
		private HttpClient httpClient = new HttpClient();
		private final CorePlugin corePlugin;

		private Builder(Metric2Registry registry, CorePlugin corePlugin) {
			super(registry, "stagemonitor-influxdb-reporter");
			this.corePlugin = corePlugin;
		}

		public HttpClient getHttpClient() {
			return httpClient;
		}

		@Override
		public InfluxDbReporter build() {
			return new InfluxDbReporter(this);
		}

		public Builder httpClient(HttpClient httpClient) {
			this.httpClient = httpClient;
			return this;
		}

		public CorePlugin getCorePlugin() {
			return corePlugin;
		}
	}

	public static String getInfluxDbLineProtocolString(MetricName metricName) {
		String influxDbString = metricNameToInfluxDBFormatCache.get(metricName);
		if (influxDbString == null) {
			final StringBuilder sb = new StringBuilder(metricName.getName().length() + metricName.getTagKeys().size() * 16 + metricName.getTagKeys().size());
			sb.append(escapeForInfluxDB(metricName.getName()));
			appendTags(sb, metricName.getTags());
			influxDbString = sb.toString();
			metricNameToInfluxDBFormatCache.put(metricName, influxDbString);
		}
		return influxDbString;
	}

	private static String getInfluxDbTags(Map<String, String> tags) {
		final StringBuilder sb = new StringBuilder();
		appendTags(sb, tags);
		return sb.toString();
	}

	private static void appendTags(StringBuilder sb, Map<String, String> tags) {
		for (String key : new TreeSet<String>(tags.keySet())) {
			sb.append(',').append(escapeForInfluxDB(key)).append('=').append(escapeForInfluxDB(tags.get(key)));
		}
	}

	private static String escapeForInfluxDB(String s) {
		if (s.indexOf(',') != -1 || s.indexOf(' ') != -1) {
			return s.replace(" ", "\\ ").replace(",", "\\,");
		}
		return s;
	}
}
