package org.stagemonitor.core;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationSource;
import org.stagemonitor.core.configuration.EnvironmentVariableConfigurationSource;
import org.stagemonitor.core.configuration.PropertyFileConfigurationSource;
import org.stagemonitor.core.configuration.SystemPropertyConfigurationSource;
import org.stagemonitor.core.metrics.MetricsWithCountFilter;
import org.stagemonitor.core.metrics.OrMetricFilter;
import org.stagemonitor.core.metrics.RegexMetricFilter;
import org.stagemonitor.core.metrics.SortedTableLogReporter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static org.stagemonitor.core.util.GraphiteSanitizer.sanitizeGraphiteMetricSegment;

public final class StageMonitor {

	public static final String STAGEMONITOR_PASSWORD = "stagemonitor.password";
	private static Logger logger = LoggerFactory.getLogger(StageMonitor.class);
	private static Configuration configuration;
	private static volatile boolean started;
	private static volatile MeasurementSession measurementSession;
	private static List<ConfigurationSource> additionalConfigurationSources = Collections.emptyList();

	static {
		reset();
	}

	private StageMonitor() {
	}

	public synchronized static void startMonitoring(MeasurementSession measurementSession,
													ConfigurationSource... additionalConfigurationSources) {
		if (!getConfiguration(CorePlugin.class).isStagemonitorActive()) {
			logger.info("stagemonitor is deactivated");
			started = true;
		}
		if (started) {
			return;
		}
		StageMonitor.measurementSession = measurementSession;
		if (measurementSession.isInitialized() && !started) {
			try {
				if (additionalConfigurationSources.length > 0) {
					StageMonitor.additionalConfigurationSources = Arrays.asList(additionalConfigurationSources);
					reloadConfiguration();
				}
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
		final CorePlugin corePlugin = getConfiguration(CorePlugin.class);
		initializePlugins(corePlugin);
		RegexMetricFilter regexFilter = new RegexMetricFilter(corePlugin.getExcludedMetricsPatterns());
		getMetricRegistry().removeMatching(regexFilter);

		MetricFilter allFilters = new OrMetricFilter(regexFilter, new MetricsWithCountFilter());

		reportToGraphite(corePlugin.getGraphiteReportingInterval(), measurementSession, allFilters, corePlugin);
		reportToConsole(corePlugin.getConsoleReportingInterval(), allFilters);
		if (corePlugin.reportToJMX()) {
			reportToJMX(allFilters);
		}
		logger.info("Measurement Session is initialized: " + measurementSession);
		started = true;
	}

	private static void initializePlugins(CorePlugin corePlugin) {
		final Collection<String> disabledPlugins = corePlugin.getDisabledPlugins();
		for (StageMonitorPlugin stagemonitorPlugin : ServiceLoader.load(StageMonitorPlugin.class)) {
			final String pluginName = stagemonitorPlugin.getClass().getSimpleName();

			if (disabledPlugins.contains(pluginName)) {
				logger.info("Not initializing disabled plugin {}", pluginName);
			} else {
				logger.info("Initializing plugin {}", pluginName);
				try {
					stagemonitorPlugin.initializePlugin(getMetricRegistry(), getConfiguration());
				} catch (Exception e) {
					logger.warn("Error while initializing plugin " + pluginName +
							" (this exception is ignored)", e);
				}
			}
		}
	}

	public static MetricRegistry getMetricRegistry() {
		return SharedMetricRegistries.getOrCreate("stagemonitor");
	}

	private static void reportToGraphite(long reportingInterval, MeasurementSession measurementSession,
										 MetricFilter filter, CorePlugin corePlugin) {
		String graphiteHostName = corePlugin.getGraphiteHostName();
		if (graphiteHostName != null && !graphiteHostName.isEmpty()) {
			GraphiteReporter.forRegistry(getMetricRegistry())
					.prefixedWith(getGraphitePrefix(measurementSession))
					.convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(filter)
					.build(new Graphite(new InetSocketAddress(graphiteHostName, corePlugin.getGraphitePort())))
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

	public static <T extends StageMonitorPlugin> T getConfiguration(Class<T> plugin) {
		return configuration.getConfig(plugin);
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

	/**
	 * Should only be used by the internal unit tests
	 */
	public static void reset() {
		reloadConfiguration();
		started = false;
		measurementSession = new MeasurementSession(null, null, null);
	}

	private static void reloadConfiguration() {
		List<ConfigurationSource> configurationSources = new ArrayList<ConfigurationSource>();
		configurationSources.addAll(additionalConfigurationSources);
		configurationSources.add(new SystemPropertyConfigurationSource());
		final String stagemonitorPropertyOverridesLocation = System.getProperty("stagemonitor.property.overrides");
		if (stagemonitorPropertyOverridesLocation != null) {
			logger.info("try loading of default property overrides: '" + stagemonitorPropertyOverridesLocation + "'");
			configurationSources.add(new PropertyFileConfigurationSource(stagemonitorPropertyOverridesLocation));
		}
		configurationSources.add(new PropertyFileConfigurationSource("stagemonitor.properties"));
		configurationSources.add(new EnvironmentVariableConfigurationSource());
		configuration = new Configuration(StageMonitorPlugin.class, configurationSources, STAGEMONITOR_PASSWORD);
	}
}
