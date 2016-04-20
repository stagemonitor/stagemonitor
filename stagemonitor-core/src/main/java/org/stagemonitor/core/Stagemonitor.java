package org.stagemonitor.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.instrument.AgentAttacher;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.ClassUtils;

public final class Stagemonitor {

	public static final String STAGEMONITOR_PASSWORD = "stagemonitor.password";
	private static Logger logger = LoggerFactory.getLogger(Stagemonitor.class);
	private static Configuration configuration;
	private static volatile boolean started;
	private static volatile boolean disabled;
	private static volatile MeasurementSession measurementSession;
	private static List<String> pathsOfWidgetMetricTabPlugins = Collections.emptyList();
	private static List<String> pathsOfWidgetTabPlugins = Collections.emptyList();
	private static Iterable<StagemonitorPlugin> plugins;
	private static List<Runnable> onShutdownActions = new CopyOnWriteArrayList<Runnable>();
	private static Metric2Registry metric2Registry = new Metric2Registry();

	static {
		try {
			reset();
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private Stagemonitor() {
	}

	/**
	 * Just makes sure the static initializer is executed
	 */
	public static void init() {
		// intentionally left blank
	}

	public synchronized static void setMeasurementSession(MeasurementSession measurementSession) {
		if (!getPlugin(CorePlugin.class).isStagemonitorActive()) {
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

	public synchronized static Future<?> startMonitoring(MeasurementSession measurementSession) {
		setMeasurementSession(measurementSession);
		return startMonitoring();
	}

	private synchronized static void doStartMonitoring() {
		if (started) {
			return;
		}
		if (measurementSession.isInitialized()) {
			logger.info("Measurement Session is initialized: " + measurementSession);
			try {
				start();
			} catch (RuntimeException e) {
				logger.warn("Error while trying to start monitoring. (this exception is ignored)", e);
			}
		} else {
			logger.warn("Measurement Session is not initialized: {}", measurementSession);
			logger.warn("make sure the properties 'stagemonitor.instanceName' and 'stagemonitor.applicationName' " +
					"are set and stagemonitor.properties is available in the classpath");
		}
	}

	private static void start() {
		initializePlugins();
		started = true;
		// don't register a shutdown hook for web applications as this causes a memory leak
		if (ClassUtils.isNotPresent("javax.servlet.Servlet")) {
			// in case the application does not directly call shutDown
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					shutDown();
				}
			}));
		}
		if (ClassUtils.isNotPresent("org.stagemonitor.requestmonitor.RequestMonitorPlugin")) {
			// if the RequestMonitorPlugin is available,
			// TypeDefinition caching should only be deactivated after the first request
			// if not, deactivate as soon as stagemonitor has started
			AgentAttacher.onMostClassesLoaded();
		}
	}

	private static void initializePlugins() {
		final CorePlugin corePlugin = getPlugin(CorePlugin.class);
		final Collection<String> disabledPlugins = corePlugin.getDisabledPlugins();
		pathsOfWidgetMetricTabPlugins = new CopyOnWriteArrayList<String>();
		pathsOfWidgetTabPlugins = new CopyOnWriteArrayList<String>();
		for (StagemonitorPlugin stagemonitorPlugin : plugins) {
			final String pluginName = stagemonitorPlugin.getClass().getSimpleName();

			if (disabledPlugins.contains(pluginName)) {
				logger.info("Not initializing disabled plugin {}", pluginName);
			} else {
				initializePlugin(stagemonitorPlugin, pluginName);
			}
		}
	}

	private static void initializePlugin(final StagemonitorPlugin stagemonitorPlugin, String pluginName) {
		logger.info("Initializing plugin {}", pluginName);
		try {
			stagemonitorPlugin.initializePlugin(new StagemonitorPlugin.InitArguments(metric2Registry, getConfiguration(), measurementSession));
			stagemonitorPlugin.initializePlugin(metric2Registry, getConfiguration());
			pathsOfWidgetMetricTabPlugins.addAll(stagemonitorPlugin.getPathsOfWidgetMetricTabPlugins());
			pathsOfWidgetTabPlugins.addAll(stagemonitorPlugin.getPathsOfWidgetTabPlugins());
			stagemonitorPlugin.registerWidgetTabPlugins(new StagemonitorPlugin.WidgetTabPluginsRegistry(pathsOfWidgetTabPlugins));
			stagemonitorPlugin.registerWidgetMetricTabPlugins(new StagemonitorPlugin.WidgetMetricTabPluginsRegistry(pathsOfWidgetMetricTabPlugins));
			onShutdownActions.add(new Runnable() {
				public void run() {
					stagemonitorPlugin.onShutDown();
				}
			});
		} catch (Exception e) {
			logger.warn("Error while initializing plugin " + pluginName + " (this exception is ignored)", e);
		}
	}

	/**
	 * Should be called when the server is shutting down.
	 * Calls the {@link StagemonitorPlugin#onShutDown()} method of all plugins
	 */
	public static synchronized void shutDown() {
		if (measurementSession.getEndTimestamp() != null) {
			// shutDown has already been called
			return;
		}
		measurementSession.setEndTimestamp(System.currentTimeMillis());
		for (Runnable onShutdownAction : onShutdownActions) {
			try {
				onShutdownAction.run();
			} catch (RuntimeException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		configuration.close();
	}

	/**
	 * @deprecated use {@link #getMetric2Registry()}
	 */
	@Deprecated
	public static MetricRegistry getMetricRegistry() {
		return metric2Registry.getMetricRegistry();
	}

	public static Metric2Registry getMetric2Registry() {
		return metric2Registry;
	}

	public static Configuration getConfiguration() {
		return configuration;
	}

	public static <T extends StagemonitorPlugin> T getPlugin(Class<T> plugin) {
		return configuration.getConfig(plugin);
	}

	/**
	 * @deprecated use {@link #getPlugin(Class)}
	 */
	@Deprecated
	public static <T extends StagemonitorPlugin> T getConfiguration(Class<T> plugin) {
		return getPlugin(plugin);
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
	 * @see StagemonitorPlugin#getPathsOfWidgetTabPlugins()
	 */
	public static List<String> getPathsOfWidgetTabPlugins() {
		return Collections.unmodifiableList(pathsOfWidgetTabPlugins);
	}
	/**
	 * @see org.stagemonitor.core.StagemonitorPlugin#getPathsOfWidgetMetricTabPlugins()
	 */
	public static List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.unmodifiableList(pathsOfWidgetMetricTabPlugins);
	}

	/**
	 * Should only be used outside of this class by the internal unit tests
	 */
	public static void reset() {
		started = false;
		disabled = false;
		measurementSession = new MeasurementSession(null, null, null);
		metric2Registry.removeMatching(Metric2Filter.ALL);
		if (configuration == null) {
			reloadPluginsAndConfiguration();
		}
		tryStartMonitoring();
		onShutdownActions.add(AgentAttacher.performRuntimeAttachment());
	}

	private static void tryStartMonitoring() {
		CorePlugin corePlugin = getPlugin(CorePlugin.class);
		MeasurementSession session = new MeasurementSession(corePlugin.getApplicationName(),
				corePlugin.getHostName(), corePlugin.getInstanceName());
		startMonitoring(session);
	}

	private static void reloadPluginsAndConfiguration() {
		List<ConfigurationSource> configurationSources = new ArrayList<ConfigurationSource>();
		for (StagemonitorConfigurationSourceInitializer initializer : ServiceLoader.load(StagemonitorConfigurationSourceInitializer.class, Stagemonitor.class.getClassLoader())) {
			initializer.modifyConfigurationSources(new StagemonitorConfigurationSourceInitializer.ModifyArguments(configurationSources));
		}
		configurationSources.remove(null);

		plugins = ServiceLoader.load(StagemonitorPlugin.class, Stagemonitor.class.getClassLoader());
		configuration = new Configuration(plugins, configurationSources, STAGEMONITOR_PASSWORD);

		try {
			for (StagemonitorConfigurationSourceInitializer initializer : ServiceLoader.load(StagemonitorConfigurationSourceInitializer.class, Stagemonitor.class.getClassLoader())) {
				initializer.onConfigurationInitialized(new StagemonitorConfigurationSourceInitializer.ConfigInitializedArguments(configuration));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error("Stagemonitor will be deactivated!");
			disabled = true;
		}
	}
}
