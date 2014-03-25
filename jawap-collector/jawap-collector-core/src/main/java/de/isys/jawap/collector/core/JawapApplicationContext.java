package de.isys.jawap.collector.core;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import de.isys.jawap.collector.core.metrics.SortedTableLogReporter;
import de.isys.jawap.entities.MeasurementSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static de.isys.jawap.util.GraphiteEncoder.encodeForGraphite;

public class JawapApplicationContext {

	private final static Log logger = LogFactory.getLog(JawapApplicationContext.class);
	private static Configuration configuration = new Configuration();

	public static boolean startMonitoring(MeasurementSession measurementSession) {
		if (measurementSession.isInitialized()) {
			initializePlugins();
			applyExcludePatterns();

			reportToGraphite(configuration.getGraphiteReportingInterval(), measurementSession);
			reportToConsole(configuration.getConsoleReportingInterval());
			if (configuration.reportToJMX()) {
				reportToJMX();
			}
			logger.info("Measurement Session is initialized: " + measurementSession);
			return true;
		} else {
			logger.warn("Measurement Session is not initialized: " + measurementSession);
			logger.warn("make sure the properties 'jawap.instanceName' and 'jawap.applicationName' are set and jawap.properties is available in the classpath");
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
		for (JawapPlugin jawapPlugin : ServiceLoader.load(JawapPlugin.class)) {
			jawapPlugin.initializePlugin(getMetricRegistry(), getConfiguration());
		}
	}

	public static MetricRegistry getMetricRegistry() {
		return SharedMetricRegistries.getOrCreate("jawap");
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
					.build(graphite)
					.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static String getGraphitePrefix(MeasurementSession measurementSession) {
		return name("jawap",
				encodeForGraphite(measurementSession.getApplicationName()),
				encodeForGraphite(measurementSession.getInstanceName()),
				encodeForGraphite(measurementSession.getHostName()));
	}

	private static void reportToConsole(long reportingInterval) {
		if (reportingInterval > 0) {
			SortedTableLogReporter.forRegistry(getMetricRegistry())
					.convertRatesTo(TimeUnit.MINUTES)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.build()
					.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static void reportToJMX() {
		JmxReporter.forRegistry(getMetricRegistry()).build().start();
	}

	public static Configuration getConfiguration() {
		return configuration;
	}
}
