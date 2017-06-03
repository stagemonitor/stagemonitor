package org.stagemonitor.core.metrics.metrics2;

import com.codahale.metrics.Gauge;

import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.MetricNameFilter;
import org.stagemonitor.core.metrics.SortedTableLogReporter;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MetricsStoreBenchmark {

	private final Metric2Registry metricRegistry;
	private final Integer reportingInterval;
	private final int reports;
	private final int gauges;
	private final int timers;
	private final int meters;

	public static void main(String[] args) throws Exception {
		final MetricsStoreBenchmark metricsStoreBenchmark = new MetricsStoreBenchmark(1, 1008, 3000, 1000, 2000);
		metricsStoreBenchmark.run();
	}

	public MetricsStoreBenchmark(Integer reportingInterval, int reports, int gauges, int timers, int meters) {
		this.reportingInterval = reportingInterval;
		this.reports = reports;
		this.gauges = gauges;
		this.timers = timers;
		this.meters = meters;
		System.setProperty("stagemonitor.reporting.elasticsearch.url", "http://192.168.99.100:9200");
		System.setProperty("stagemonitor.reporting.interval.elasticsearch", reportingInterval.toString());
		System.setProperty("stagemonitor.reporting.influxdb.url", "http://192.168.99.100:8086");
		System.setProperty("stagemonitor.reporting.interval.influxdb", reportingInterval.toString());
		System.setProperty("stagemonitor.reporting.interval.aggregation", "-1");
		System.setProperty("stagemonitor.applicationName", "Metrics Store Benchmark");
		System.setProperty("stagemonitor.instanceName", "instance");

		metricRegistry = Stagemonitor.getMetric2Registry();
	}

	private void run() throws InterruptedException {
		registerGauges(gauges);

		Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				final Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		}).scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				updateMeters(meters);
				updateTimers(timers);
			}
		}, 0, 500, TimeUnit.MILLISECONDS);

		final SortedTableLogReporter logReporter = SortedTableLogReporter
				.forRegistry(metricRegistry)
				.filter(MetricNameFilter.includePatterns(name("reporting_time").build()))
				.log(LoggerFactory.getLogger(MetricsStoreBenchmark.class))
				.build();
		logReporter.start(10, TimeUnit.SECONDS);

		Thread.sleep((reportingInterval * reports * 1000) + reportingInterval);

		logReporter.report();
	}

	private void updateTimers(int numberOfTimers) {
		for (int i = 0; i < numberOfTimers; i++) {
			metricRegistry.timer(name("timer").tag("number", Integer.toString(i)).build()).update((long) (Math.random() * 1000), TimeUnit.MILLISECONDS);
		}
	}

	private void updateMeters(int n) {
		for (int i = 0; i < n; i++) {
			metricRegistry.meter(name("meter").tag("number", Integer.toString(i)).build()).mark();
		}
	}

	private void registerGauges(int n) {
		for (int i = 0; i < n; i++) {
			metricRegistry.register(name("gauge").tag("number", Integer.toString(i)).build(), new Gauge<Double>() {
				@Override
				public Double getValue() {
					return Math.random();
				}
			});
		}
	}

}
