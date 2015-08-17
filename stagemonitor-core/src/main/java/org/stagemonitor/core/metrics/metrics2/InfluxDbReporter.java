package org.stagemonitor.core.metrics.metrics2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.HttpClient;

public class InfluxDbReporter extends ScheduledMetrics2Reporter {

	private static final int MAX_BATCH_SIZE = 5000;

	private List<String> batchLines = new ArrayList<String>(MAX_BATCH_SIZE);
	private final String globalTags;
	private HttpClient httpClient;
	private final Clock clock;
	private final CorePlugin corePlugin;

	public InfluxDbReporter(Metric2Registry registry,
							   Metric2Filter filter,
							   TimeUnit rateUnit,
							   TimeUnit durationUnit,
							   Map<String, String> globalTags,
							   HttpClient httpClient,
							   CorePlugin corePlugin) {

		this(registry, filter, rateUnit, durationUnit, globalTags, httpClient, Clock.defaultClock(), corePlugin);
	}

	public InfluxDbReporter(Metric2Registry registry,
							   Metric2Filter filter,
							   TimeUnit rateUnit,
							   TimeUnit durationUnit,
							   Map<String, String> globalTags,
							   HttpClient httpClient,
							   Clock clock, CorePlugin corePlugin) {

		super(registry, filter, rateUnit, durationUnit);
		this.corePlugin = corePlugin;
		this.globalTags = MetricName.getInfluxDbTags(globalTags);
		this.httpClient = httpClient;
		this.clock = clock;
	}

	@Override
	public void reportMetrics(Map<MetricName, Gauge> gauges,
							  Map<MetricName, Counter> counters,
							  Map<MetricName, Histogram> histograms,
							  Map<MetricName, Meter> meters,
							  Map<MetricName, Timer> timers) {

		long timestamp = clock.getTime();
		reportGauges(gauges, timestamp);
		reportCounter(counters, timestamp);
		reportHistograms(histograms, timestamp);
		reportMeters(meters, timestamp);
		reportTimers(timers, timestamp);
		flush();
	}

	private void reportGauges(Map<MetricName, Gauge> gauges, long timestamp) {
		for (Map.Entry<MetricName, Gauge> entry : gauges.entrySet()) {
			reportLine(entry.getKey().getInfluxDbLineProtocolString(),
					"value=" + getValueForInfluxDb(entry.getValue().getValue()), timestamp);
		}
	}

	private void reportCounter(Map<MetricName, Counter> counters, long timestamp) {
		for (Map.Entry<MetricName, Counter> entry : counters.entrySet()) {
			final Counter counter = entry.getValue();
			reportLine(entry.getKey().getInfluxDbLineProtocolString(),
					"count=" + getIntegerValue(counter.getCount()), timestamp);
		}
	}

	private void reportHistograms(Map<MetricName, Histogram> histograms, long timestamp) {
		for (Map.Entry<MetricName, Histogram> entry : histograms.entrySet()) {
			final Histogram hist = entry.getValue();
			final Snapshot snapshot = hist.getSnapshot();
			reportLine(entry.getKey().getInfluxDbLineProtocolString(),
					"count=" + getIntegerValue(hist.getCount()) + ","
							+ reportSnapshot(snapshot), timestamp);
		}
	}

	private void reportMeters(Map<MetricName, Meter> meters, long timestamp) {
		for (Map.Entry<MetricName, Meter> entry : meters.entrySet()) {
			final Meter meter = entry.getValue();
			reportLine(entry.getKey().getInfluxDbLineProtocolString(), reportMetered(meter), timestamp);
		}
	}

	private void reportTimers(Map<MetricName, Timer> timers, long timestamp) {
		for (Map.Entry<MetricName, Timer> entry : timers.entrySet()) {
			final Timer timer = entry.getValue();
			final Snapshot snapshot = timer.getSnapshot();
			reportLine(entry.getKey().getInfluxDbLineProtocolString(),
					reportMetered(timer) + ","
							+ reportSnapshot(snapshot), timestamp);
		}
	}

	private String reportSnapshot(Snapshot snapshot) {
		return "min=" + getDuration(snapshot.getMin()) + ","
				+ "max=" + getDuration(snapshot.getMax()) + ","
				+ "mean=" + getDuration(snapshot.getMean()) + ","
				+ "median=" + getDuration(snapshot.getMedian()) + ","
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

	private String getValueForInfluxDb(Object value) {
		if (value instanceof Integer || value instanceof Long) {
			return getIntegerValue(value);
		} else if (value instanceof Number) {
			return getFloatValue(value);
		} else if (value instanceof Boolean) {
			return value.toString();
		} else {
			return getStringValue(Objects.toString(value));
		}
	}

	private String getIntegerValue(Object integer) {
		return integer.toString();
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
			return "null";
		} else {
			return result;
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
}
