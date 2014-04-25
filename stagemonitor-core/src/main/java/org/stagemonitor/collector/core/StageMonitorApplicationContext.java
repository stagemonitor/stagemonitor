package org.stagemonitor.collector.core;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.stagemonitor.collector.core.metrics.SortedTableLogReporter;
import org.stagemonitor.collector.core.rest.RestClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.collector.core.util.GraphiteEncoder.encodeForGraphite;

public class StageMonitorApplicationContext {

	private final static Log logger = LogFactory.getLog(StageMonitorApplicationContext.class);
	private static Configuration configuration = new Configuration();
	private static AtomicBoolean started = new AtomicBoolean(false);

	public synchronized static boolean startMonitoring(MeasurementSession measurementSession) {
		if (started.get()) {
			return true;
		}
		//addElasticsearchMapping();
		if (measurementSession.isInitialized() && !started.get()) {
			initializePlugins();
			applyExcludePatterns();

			reportToGraphite(configuration.getGraphiteReportingInterval(), measurementSession);
			reportToConsole(configuration.getConsoleReportingInterval());
			if (configuration.reportToJMX()) {
				reportToJMX();
			}
			logger.info("Measurement Session is initialized: " + measurementSession);
			started.set(true);
			return true;
		} else {
			logger.warn("Measurement Session is not initialized: " + measurementSession);
			logger.warn("make sure the properties 'stagemonitor.instanceName' and 'stagemonitor.applicationName' are set and stagemonitor.properties is available in the classpath");
			return false;
		}
	}

	// TODO
	private static void addElasticsearchMapping() {
		try {
			final HttpURLConnection response = RestClient.get(configuration.getServerUrl());
			final int responseCode = response.getResponseCode();
			if (responseCode == 404) {

			} else if (responseCode >= 400) {
				logger.error(String.format("Unexpected elastic search error (%d): %s", responseCode, response.getContent().toString()));
			}
		} catch (IOException e) {

		}
	}

	private static void applyExcludePatterns() {
		for (final String excludePattern : configuration.getExcludedMetricsPatterns()) {
			getMetricRegistry().removeMatching(new MetricFilter() {
				@Override
				public boolean matches(String name, Metric metric) {
					return name.matches(excludePattern);
				}
			});
		}
	}

	private static void initializePlugins() {
		for (StageMonitorPlugin stagemonitorPlugin : ServiceLoader.load(StageMonitorPlugin.class)) {
			stagemonitorPlugin.initializePlugin(getMetricRegistry(), getConfiguration());
		}
	}

	public static MetricRegistry getMetricRegistry() {
		return SharedMetricRegistries.getOrCreate("stagemonitor");
	}

	private static void reportToGraphite(long reportingInterval, MeasurementSession measurementSession) {
		String graphiteHostName = configuration.getGraphiteHostName();
		if (graphiteHostName != null && !graphiteHostName.isEmpty()) {
			final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHostName,
					configuration.getGraphitePort()));
			GraphiteReporter.forRegistry(getMetricRegistry())
					.prefixedWith(getGraphitePrefix(measurementSession))
					.convertRatesTo(TimeUnit.MINUTES)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(new MetricsWithCountFilter())
					.build(graphite)
					.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static String getGraphitePrefix(MeasurementSession measurementSession) {
		return name("stagemonitor",
				encodeForGraphite(measurementSession.getApplicationName()),
				encodeForGraphite(measurementSession.getInstanceName()),
				encodeForGraphite(measurementSession.getHostName()));
	}

	private static void reportToConsole(long reportingInterval) {
		if (reportingInterval > 0) {
			SortedTableLogReporter.forRegistry(getMetricRegistry())
					.convertRatesTo(TimeUnit.MINUTES)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(new MetricsWithCountFilter())
					.build()
					.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static void reportToJMX() {
		JmxReporter.forRegistry(getMetricRegistry())
				.filter(new MetricsWithCountFilter())
				.build().start();
	}

	public static Configuration getConfiguration() {
		return configuration;
	}

	private static class MetricsWithCountFilter implements MetricFilter {
		@Override
		public boolean matches(String name, Metric metric) {
			if (metric instanceof Metered) {
				return ((Metered) metric).getCount() > 0;
			}
			return true;
		}
	}
}
