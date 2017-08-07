package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.ConfigurationSource;
import org.stagemonitor.core.instrument.AgentAttacher;
import org.stagemonitor.core.metrics.health.ImmediateResult;
import org.stagemonitor.core.metrics.health.OverridableHealthCheckRegistry;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.ClassUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Stagemonitor {

	public static final String STAGEMONITOR_PASSWORD = "stagemonitor.password";
	private static Logger logger = LoggerFactory.getLogger(Stagemonitor.class);
	private static ConfigurationRegistry configuration;
	private static volatile boolean started;
	private static volatile boolean disabled;
	private static volatile MeasurementSession measurementSession;
	private static List<String> pathsOfWidgetMetricTabPlugins = Collections.emptyList();
	private static List<String> pathsOfWidgetTabPlugins = Collections.emptyList();
	private static Iterable<StagemonitorPlugin> plugins;
	private static List<Runnable> onShutdownActions = new CopyOnWriteArrayList<Runnable>();
	private static Metric2Registry metric2Registry = new Metric2Registry(SharedMetricRegistries.getOrCreate("stagemonitor"));
	private static HealthCheckRegistry healthCheckRegistry = new OverridableHealthCheckRegistry();

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

	public static synchronized void setMeasurementSession(MeasurementSession measurementSession) {
		if (!getPlugin(CorePlugin.class).isStagemonitorActive()) {
			logger.info("stagemonitor is deactivated");
			disabled = true;
		}
		if (started || disabled) {
			return;
		}
		Stagemonitor.measurementSession = measurementSession;
	}

	public static void startMonitoring() {
		doStartMonitoring();
	}

	public static synchronized void startMonitoring(MeasurementSession measurementSession) {
		setMeasurementSession(measurementSession);
		startMonitoring();
	}

	private static synchronized void doStartMonitoring() {
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
			logger.debug("Measurement Session is not initialized: {}", measurementSession);
			logger.debug("make sure the properties 'stagemonitor.instanceName' and 'stagemonitor.applicationName' " +
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

		logStatus();
		logConfiguration();
	}

	private static void logStatus() {
		logger.info("# stagemonitor status");
		for (Map.Entry<String, HealthCheck.Result> entry : healthCheckRegistry.runHealthChecks().entrySet()) {
			String status = entry.getValue().isHealthy() ? "OK  " : "FAIL";
			String message = entry.getValue().getMessage() == null ? "" : "(" + entry.getValue().getMessage() + ")";
			final String checkName = entry.getKey();
			logger.info("{} - {} {}", status, checkName, message);
			final Throwable error = entry.getValue().getError();
			if (error != null) {
				logger.warn("Exception thrown while initializing plugin", error);
			}
		}
	}

	private static void logConfiguration() {
		logger.info("# stagemonitor configuration, listing non-default values:");
		boolean hasOnlyDefaultOptions = true;

		for (List<ConfigurationOption<?>> options : configuration.getConfigurationOptionsByCategory().values()) {
			for (ConfigurationOption<?> option : options) {
				if (!option.isDefault()) {
					hasOnlyDefaultOptions = false;
					String trimmedValue;
					if (option.isSensitive()) {
						trimmedValue = "XXXX";
					} else {
						trimmedValue = option.getValueAsString().replace("\n", "");
						int maximumLineLength = 55;
						if (trimmedValue.length() > maximumLineLength) {
							trimmedValue = trimmedValue.substring(0, maximumLineLength - 3) + "...";
						}
					}
					logger.info("{}: {} (source: {})", option.getKey(), trimmedValue, option.getNameOfCurrentConfigurationSource());
				}
			}
		}

		if (hasOnlyDefaultOptions) {
			logger.warn("stagemonitor has not been configured. Have a look at");
			logger.warn("https://github.com/stagemonitor/stagemonitor/wiki/How-should-I-configure-stagemonitor%3F");
			logger.warn("and");
			logger.warn("https://github.com/stagemonitor/stagemonitor/wiki/Configuration-Options");
			logger.warn("for further instructions");
		}
	}

	private static void initializePlugins() {
		final CorePlugin corePlugin = getPlugin(CorePlugin.class);
		final Collection<String> disabledPlugins = corePlugin.getDisabledPlugins();
		pathsOfWidgetMetricTabPlugins = new CopyOnWriteArrayList<String>();
		pathsOfWidgetTabPlugins = new CopyOnWriteArrayList<String>();

		initializePluginsInOrder(disabledPlugins, plugins);
	}

	static void initializePluginsInOrder(Collection<String> disabledPlugins, Iterable<StagemonitorPlugin> plugins) {
		Set<Class<? extends StagemonitorPlugin>> alreadyInitialized = new HashSet<Class<? extends StagemonitorPlugin>>();
		Set<StagemonitorPlugin> notYetInitialized = getPluginsToInit(disabledPlugins, plugins);
		while (!notYetInitialized.isEmpty()) {
			int countNotYetInitialized = notYetInitialized.size();
			// try to init plugins which are
			for (Iterator<StagemonitorPlugin> iterator = notYetInitialized.iterator(); iterator.hasNext(); ) {
				StagemonitorPlugin stagemonitorPlugin = iterator.next();
				{
					final List<Class<? extends StagemonitorPlugin>> dependencies = stagemonitorPlugin.dependsOn();
					if (dependencies.isEmpty() || alreadyInitialized.containsAll(dependencies)) {
						initializePlugin(stagemonitorPlugin);
						iterator.remove();
						alreadyInitialized.add(stagemonitorPlugin.getClass());
					}
				}
			}
			if (countNotYetInitialized == notYetInitialized.size()) {
				// no plugins could be initialized in this try. this probably means there is a cyclic dependency
				throw new IllegalStateException("Cyclic dependencies detected: " + notYetInitialized);
			}
		}
	}

	private static Set<StagemonitorPlugin> getPluginsToInit(Collection<String> disabledPlugins, Iterable<StagemonitorPlugin> plugins) {
		Set<StagemonitorPlugin> notYetInitialized = new HashSet<StagemonitorPlugin>();
		for (StagemonitorPlugin stagemonitorPlugin : plugins) {
			final String pluginName = stagemonitorPlugin.getClass().getSimpleName();
			if (disabledPlugins.contains(pluginName)) {
				logger.info("Not initializing disabled plugin {}", pluginName);
				healthCheckRegistry.register(pluginName, ImmediateResult.of(HealthCheck.Result.unhealthy("disabled via configuration")));
			} else {
				notYetInitialized.add(stagemonitorPlugin);
			}
		}
		return notYetInitialized;
	}

	private static void initializePlugin(final StagemonitorPlugin stagemonitorPlugin) {
		final String pluginName = stagemonitorPlugin.getClass().getSimpleName();
		try {
			stagemonitorPlugin.initializePlugin(new StagemonitorPlugin.InitArguments(metric2Registry, getConfiguration(), measurementSession, healthCheckRegistry));
			stagemonitorPlugin.initialized = true;
			for (Runnable onInitCallback : stagemonitorPlugin.onInitCallbacks) {
				onInitCallback.run();
			}
			stagemonitorPlugin.registerWidgetTabPlugins(new StagemonitorPlugin.WidgetTabPluginsRegistry(pathsOfWidgetTabPlugins));
			stagemonitorPlugin.registerWidgetMetricTabPlugins(new StagemonitorPlugin.WidgetMetricTabPluginsRegistry(pathsOfWidgetMetricTabPlugins));
			healthCheckRegistry.register(pluginName, ImmediateResult.of(HealthCheck.Result.healthy("initialized successfully")));
		} catch (final Exception e) {
			healthCheckRegistry.register(pluginName, ImmediateResult.of(HealthCheck.Result.unhealthy(e)));
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
		for (StagemonitorPlugin plugin : plugins) {
			try {
				plugin.onShutDown();
			} catch (Exception e) {
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

	public static HealthCheckRegistry getHealthCheckRegistry() {
		return healthCheckRegistry;
	}

	public static ConfigurationRegistry getConfiguration() {
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

	static void setConfiguration(ConfigurationRegistry configuration) {
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
		if (configuration == null) {
			reloadPluginsAndConfiguration();
		}
		onShutdownActions.add(AgentAttacher.performRuntimeAttachment());
		tryStartMonitoring();
		healthCheckRegistry.register("Startup", new HealthCheck() {
			@Override
			protected Result check() throws Exception {
				if (started) {
					return Result.healthy();
				} else {
					return Result.unhealthy("stagemonitor is not started");
				}
			}
		});
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
		configuration = new ConfigurationRegistry(plugins, configurationSources, STAGEMONITOR_PASSWORD);

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
