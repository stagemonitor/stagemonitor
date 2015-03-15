package org.stagemonitor.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

public final class Stagemonitor {

	public static final String STAGEMONITOR_PASSWORD = "stagemonitor.password";
	private static Logger logger = LoggerFactory.getLogger(Stagemonitor.class);
	private static Configuration configuration;
	private static volatile boolean started;
	private static volatile boolean disabled;
	private static volatile MeasurementSession measurementSession;
	private static List<String> pathsOfWidgetMetricTabPlugins = Collections.emptyList();
	private static Iterable<StagemonitorPlugin> plugins;

	static {
		reset();
	}

	private Stagemonitor() {
	}

	public synchronized static void setMeasurementSession(MeasurementSession measurementSession) {
		if (!getConfiguration(CorePlugin.class).isStagemonitorActive()) {
			logger.info("stagemonitor is deactivated");
			disabled = true;
		}
		if (started || disabled) {
			return;
		}
		Stagemonitor.measurementSession = measurementSession;
	}

	public static Future<?> startMonitoring() {
		ExecutorService startupThread = Executors.newSingleThreadExecutor();
		try {
			return startupThread.submit(new Runnable() {
				@Override
				public void run() {
					doStartMonitoring();
				}
			});
		} finally {
			startupThread.shutdown();
		}
	}

	private synchronized static void doStartMonitoring() {
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

	public synchronized static Future<?> startMonitoring(MeasurementSession measurementSession) {
		setMeasurementSession(measurementSession);
		return startMonitoring();
	}

	private static void start(MeasurementSession measurementSession) {
		initializePlugins();
		logger.info("Measurement Session is initialized: " + measurementSession);
		started = true;
	}

	private static void initializePlugins() {
		final CorePlugin corePlugin = getConfiguration(CorePlugin.class);
		final Collection<String> disabledPlugins = corePlugin.getDisabledPlugins();
		pathsOfWidgetMetricTabPlugins = new LinkedList<String>();
		for (StagemonitorPlugin stagemonitorPlugin : plugins) {
			final String pluginName = stagemonitorPlugin.getClass().getSimpleName();

			if (disabledPlugins.contains(pluginName)) {
				logger.info("Not initializing disabled plugin {}", pluginName);
			} else {
				initializePlugin(stagemonitorPlugin, pluginName);
			}
		}
	}

	private static void initializePlugin(StagemonitorPlugin stagemonitorPlugin, String pluginName) {
		logger.info("Initializing plugin {}", pluginName);
		try {
			stagemonitorPlugin.initializePlugin(getMetricRegistry(), getConfiguration());
			pathsOfWidgetMetricTabPlugins.addAll(stagemonitorPlugin.getPathsOfWidgetMetricTabPlugins());
		} catch (Exception e) {
			logger.warn("Error while initializing plugin " + pluginName + " (this exception is ignored)", e);
		}
	}

	/**
	 * Should be called when the server is shutting down.
	 * Calls the {@link StagemonitorPlugin#onShutDown()} method of all plugins
	 */
	public static void shutDown() {
		measurementSession.setEndTimestamp(System.currentTimeMillis());
		for (StagemonitorPlugin stagemonitorPlugin : ServiceLoader.load(StagemonitorPlugin.class)) {
			stagemonitorPlugin.onShutDown();
		}
	}

	public static MetricRegistry getMetricRegistry() {
		return SharedMetricRegistries.getOrCreate("stagemonitor");
	}

	public static Configuration getConfiguration() {
		return configuration;
	}

	public static <T extends StagemonitorPlugin> T getConfiguration(Class<T> plugin) {
		return configuration.getConfig(plugin);
	}

	static void setConfiguration(Configuration configuration) {
		Stagemonitor.configuration = configuration;
	}

	public static MeasurementSession getMeasurementSession() {
		return measurementSession;
	}

	public static boolean isStarted() {
		return started;
	}

	static boolean isDisabled() {
		return disabled;
	}

	static void setLogger(Logger logger) {
		Stagemonitor.logger = logger;
	}

	/**
	 * @see org.stagemonitor.core.StagemonitorPlugin#getPathsOfWidgetMetricTabPlugins()
	 */
	public static List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.unmodifiableList(pathsOfWidgetMetricTabPlugins);
	}

	/**
	 * Should only be used by the internal unit tests
	 */
	public static void reset() {
		started = false;
		disabled = false;
		measurementSession = new MeasurementSession(null, null, null);
		reloadConfiguration();
	}

	private static void reloadConfiguration() {
		List<ConfigurationSource> configurationSources = new ArrayList<ConfigurationSource>();
		for (StagemonitorConfigurationSourceInitializer initializer : ServiceLoader.load(StagemonitorConfigurationSourceInitializer.class)) {
			initializer.modifyConfigurationSources(configurationSources);
		}
		configurationSources.remove(null);

		plugins = ServiceLoader.load(StagemonitorPlugin.class);
		configuration = new Configuration(plugins, configurationSources, STAGEMONITOR_PASSWORD);

		try {
			for (StagemonitorConfigurationSourceInitializer initializer : ServiceLoader.load(StagemonitorConfigurationSourceInitializer.class)) {
				initializer.onConfigurationInitialized(configuration);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("Stagemonitor will be deactivated!");
			disabled = true;
		}
	}
}
