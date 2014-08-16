package org.stagemonitor.core;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.metrics.MetricsWithCountFilter;
import org.stagemonitor.core.metrics.OrMetricFilter;
import org.stagemonitor.core.metrics.RegexMetricFilter;
import org.stagemonitor.core.metrics.SortedTableLogReporter;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

public final class StageMonitor {

	private static Logger logger = LoggerFactory.getLogger(StageMonitor.class);
	private static Configuration configuration = new Configuration();
	private static volatile boolean started = false;
	private static volatile MeasurementSession measurementSession = new MeasurementSession(null, null, null);

	private StageMonitor() {
	}

	public synchronized static void startMonitoring(MeasurementSession measurementSession) {
		if (!configuration.isStagemonitorActive()) {
			logger.info("stagemonitor is deactivated");
			started = true;
		}
		if (started) {
			return;
		}
		StageMonitor.measurementSession = measurementSession;
		if (measurementSession.isInitialized() && !started) {
			try {
				start(measurementSession);
			} catch (RuntimeException e) {
				logger.warn("Error while trying to start monitoring. (this exception is ignored)", e);
			}
		} else {
			logger.warn("Measurement Session is not initialized: {}", measurementSession);
			logger.warn("make sure the properties 'stagemonitor.instanceName' and 'stagemonitor.applicationName' " +
					"are set and stagemonitor.properties is available in the classpath");
		}
	}

	private static void start(MeasurementSession measurementSession) {
		initializePlugins();
		RegexMetricFilter regexFilter = new RegexMetricFilter(configuration.getExcludedMetricsPatterns());
		getMetricRegistry().removeMatching(regexFilter);

		MetricFilter allFilters = new OrMetricFilter(regexFilter, new MetricsWithCountFilter());

		reportToGraphite(configuration.getGraphiteReportingInterval(), measurementSession, allFilters);
		reportToConsole(configuration.getConsoleReportingInterval(), allFilters);
		if (configuration.reportToJMX()) {
			reportToJMX(allFilters);
		}
		logger.info("Measurement Session is initialized: " + measurementSession);
		started = true;
	}

	private static void initializePlugins() {
		final Collection<String> disabledPlugins = configuration.getDisabledPlugins();
		for (StageMonitorPlugin stagemonitorPlugin : ServiceLoader.load(StageMonitorPlugin.class)) {
			final String pluginName = stagemonitorPlugin.getClass().getSimpleName();

			if (disabledPlugins.contains(pluginName)) {
				logger.info("Not initializing disabled plugin {}", pluginName);
			} else {
				logger.info("Initializing plugin {}", pluginName);
				try {
					stagemonitorPlugin.initializePlugin(getMetricRegistry(), getConfiguration());
				} catch (RuntimeException e) {
					logger.warn("Error while initializing plugin " + pluginName +
							" (this exception is ignored)", e);
				}
			}
		}
	}

	public static MetricRegistry getMetricRegistry() {
		return SharedMetricRegistries.getOrCreate("stagemonitor");
	}

	private static void reportToGraphite(long reportingInterval, MeasurementSession measurementSession, MetricFilter filter) {
		String graphiteHostName = configuration.getGraphiteHostName();
		if (graphiteHostName != null && !graphiteHostName.isEmpty()) {
			GraphiteReporter.forRegistry(getMetricRegistry())
					.prefixedWith(getGraphitePrefix(measurementSession))
					.convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(filter)
					.build(new Graphite(new InetSocketAddress(graphiteHostName, configuration.getGraphitePort())))
					.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static String getGraphitePrefix(MeasurementSession measurementSession) {
		return name("stagemonitor",
				sanitizeGraphiteMetricSegment(measurementSession.getApplicationName()),
				sanitizeGraphiteMetricSegment(measurementSession.getInstanceName()),
				sanitizeGraphiteMetricSegment(measurementSession.getHostName()));
	}

	private static void reportToConsole(long reportingInterval, MetricFilter filter) {
		if (reportingInterval > 0) {
			SortedTableLogReporter.forRegistry(getMetricRegistry())
					.convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(filter)
					.build()
					.start(reportingInterval, TimeUnit.SECONDS);
		}
	}

	private static void reportToJMX(MetricFilter filter) {
		JmxReporter.forRegistry(getMetricRegistry())
				.filter(filter)
				.build().start();
	}

	public static Configuration getConfiguration() {
		return configuration;
	}

	static void setConfiguration(Configuration configuration) {
		StageMonitor.configuration = configuration;
	}

	public static MeasurementSession getMeasurementSession() {
		return measurementSession;
	}

	static boolean isStarted() {
		return started;
	}

	static void setLogger(Logger logger) {
		StageMonitor.logger = logger;
	}

	static void reset() {
		configuration = new Configuration();
		started = false;
		measurementSession = new MeasurementSession(null, null, null);
	}
}
