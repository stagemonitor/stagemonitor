package org.stagemonitor.collector.core;

import com.codahale.metrics.*;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.collector.core.metrics.SortedTableLogReporter;

import java.net.InetSocketAddress;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.collector.core.util.GraphiteEncoder.encodeForGraphite;

public class StageMonitorApplicationContext {

	private final static Logger logger = LoggerFactory.getLogger(StageMonitorApplicationContext.class);
	private static Configuration configuration = new Configuration();
	private static volatile boolean started = false;

	public synchronized static boolean startMonitoring(MeasurementSession measurementSession) {
		if (started) {
			return true;
		}
		if (measurementSession.isInitialized() && !started) {
			initializePlugins();
			applyExcludePatterns();

			reportToGraphite(configuration.getGraphiteReportingInterval(), measurementSession);
			reportToConsole(configuration.getConsoleReportingInterval());
			if (configuration.reportToJMX()) {
				reportToJMX();
			}
			logger.info("Measurement Session is initialized: " + measurementSession);
			started = true;
			return true;
		} else {
			logger.warn("Measurement Session is not initialized: " + measurementSession);
			logger.warn("make sure the properties 'stagemonitor.instanceName' and 'stagemonitor.applicationName' are set and stagemonitor.properties is available in the classpath");
			return false;
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
