package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class Configuration {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final String updateConfigPasswordKey;
	private final List<ConfigurationSource> configurationSources = new CopyOnWriteArrayList<ConfigurationSource>();
	private final boolean failOnMissingRequiredValues;

	private Map<Class<? extends ConfigurationOptionProvider>, ConfigurationOptionProvider> optionProvidersByClass = new HashMap<Class<? extends ConfigurationOptionProvider>, ConfigurationOptionProvider>();
	private Map<String, ConfigurationOption<?>> configurationOptionsByKey = new LinkedHashMap<String, ConfigurationOption<?>>();
	private Map<String, List<ConfigurationOption<?>>> configurationOptionsByCategory = new LinkedHashMap<String, List<ConfigurationOption<?>>>();
	private ScheduledExecutorService configurationReloader = Executors.newScheduledThreadPool(1, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("configuration-reloader");
			return thread;
		}
	});

	/**
	 * @param updateConfigPasswordKey the key of the password to update configuration settings.
	 *                                The actual password is loaded from the configuration sources. Set to null to disable dynamic updates.
	 */
	public Configuration(String updateConfigPasswordKey) {
		this(ConfigurationOptionProvider.class, updateConfigPasswordKey);
	}

	/**
	 * @param optionProviderClass the class that should be used to lookup instances of
	 *                            {@link ConfigurationOptionProvider} via {@link ServiceLoader#load(Class)}
	 */
	public Configuration(Class<? extends ConfigurationOptionProvider> optionProviderClass) {
		this(optionProviderClass, null);
	}

	/**
	 * @param optionProviderClass     the class that should be used to lookup instances of
	 *                                {@link ConfigurationOptionProvider} via {@link ServiceLoader#load(Class)}
	 * @param updateConfigPasswordKey the key of the password to update configuration settings.
	 *                                The actual password is loaded from the configuration sources. Set to null to disable dynamic updates.
	 */
	public Configuration(Class<? extends ConfigurationOptionProvider> optionProviderClass, String updateConfigPasswordKey) {
		this(optionProviderClass, Collections.<ConfigurationSource>emptyList(), updateConfigPasswordKey);
	}

	/**
	 * @param optionProviderClass     the class that should be used to lookup instances of
	 *                                {@link ConfigurationOptionProvider} via {@link ServiceLoader#load(Class)}
	 * @param configSources           the configuration sources
	 * @param updateConfigPasswordKey the key of the password to update configuration settings.
	 *                                The actual password is loaded from the configuration sources. Set to null to disable dynamic updates.
	 */
	public Configuration(Class<? extends ConfigurationOptionProvider> optionProviderClass,
						 List<ConfigurationSource> configSources, String updateConfigPasswordKey) {
		this(ServiceLoader.load(optionProviderClass, Configuration.class.getClassLoader()), configSources, updateConfigPasswordKey);
	}

	/**
	 * @param optionProviders         the option providers
	 * @param configSources           the configuration sources
	 * @param updateConfigPasswordKey the key of the password to update configuration settings.
	 *                                The actual password is loaded from the configuration sources. Set to null to disable dynamic updates.
	 */
	public Configuration(Iterable<? extends ConfigurationOptionProvider> optionProviders,
						 List<ConfigurationSource> configSources, String updateConfigPasswordKey) {
		this(optionProviders, configSources, updateConfigPasswordKey, false);
	}

	/**
	 * @param optionProviders         the option providers
	 * @param configSources           the configuration sources
	 * @param updateConfigPasswordKey the key of the password to update configuration settings.
	 *                                The actual password is loaded from the configuration sources. Set to null to disable dynamic updates.
	 */
	public Configuration(Iterable<? extends ConfigurationOptionProvider> optionProviders,
						 List<ConfigurationSource> configSources, String updateConfigPasswordKey, boolean failOnMissingRequiredValues) {
		this.updateConfigPasswordKey = updateConfigPasswordKey;
		this.failOnMissingRequiredValues = failOnMissingRequiredValues;
		configurationSources.addAll(configSources);
		registerConfigurationOptions(optionProviders);
	}

	private void registerConfigurationOptions(Iterable<? extends ConfigurationOptionProvider> optionProviders) {
		for (ConfigurationOptionProvider configurationOptionProvider : optionProviders) {
			registerOptionProvider(configurationOptionProvider);
		}
	}

	private void registerOptionProvider(ConfigurationOptionProvider configurationOptionProvider) {
		optionProvidersByClass.put(configurationOptionProvider.getClass(), configurationOptionProvider);
		for (ConfigurationOption<?> configurationOption : configurationOptionProvider.getConfigurationOptions()) {
			add(configurationOption);
		}
	}

	/**
	 * Returns a {@link ConfigurationOptionProvider} whose {@link ConfigurationOption}s are populated
	 *
	 * @param configClass the {@link ConfigurationOptionProvider} class
	 * @param <T>         the type
	 * @return a {@link ConfigurationOptionProvider} whose {@link ConfigurationOption}s are populated
	 */
	public <T extends ConfigurationOptionProvider> T getConfig(Class<T> configClass) {
		final T config = (T) optionProvidersByClass.get(configClass);
		if (config != null) {
			return config;
		} else {
			for (Class<? extends ConfigurationOptionProvider> storedConfigClass : optionProvidersByClass.keySet()) {
				if (configClass.isAssignableFrom(storedConfigClass)) {
					return (T) optionProvidersByClass.get(storedConfigClass);
				}
			}
			return null;
		}
	}

	public Collection<ConfigurationOptionProvider> getConfigurationOptionProviders() {
		return Collections.unmodifiableCollection(optionProvidersByClass.values());
	}

	private void add(final ConfigurationOption<?> configurationOption) {
		configurationOption.setConfiguration(this);
		configurationOption.setConfigurationSources(configurationSources);

		final String key = configurationOption.getKey();
		addConfigurationOptionByKey(configurationOption, key);
		for (String alternateKey : configurationOption.getAliasKeys()) {
			addConfigurationOptionByKey(configurationOption, alternateKey);
		}
		addConfigurationOptionByCategory(configurationOption.getConfigurationCategory(), configurationOption);
	}

	private void addConfigurationOptionByKey(ConfigurationOption<?> configurationOption, String key) {
		if (configurationOptionsByKey.containsKey(key)) {
			throw new IllegalArgumentException(String.format("The configuration key %s is registered twice. Once for %s and once for %s.",
					key, configurationOptionsByKey.get(key).getLabel(), configurationOption.getLabel()));
		}
		configurationOptionsByKey.put(key, configurationOption);
	}

	private void addConfigurationOptionByCategory(String configurationCategory, final ConfigurationOption<?> configurationOption) {
		if (configurationOptionsByCategory.containsKey(configurationCategory)) {
			configurationOptionsByCategory.get(configurationCategory).add(configurationOption);
		} else {
			configurationOptionsByCategory.put(configurationCategory, new ArrayList<ConfigurationOption<?>>() {{
				add(configurationOption);
			}});
		}
	}

	/**
	 * Returns all Configuration options grouped by {@link ConfigurationOption#configurationCategory}
	 *
	 * @return all Configuration options grouped by {@link ConfigurationOption#configurationCategory}
	 */
	public Map<String, List<ConfigurationOption<?>>> getConfigurationOptionsByCategory() {
		return Collections.unmodifiableMap(configurationOptionsByCategory);
	}

	/**
	 * Returns all Configuration options grouped by the {@link ConfigurationOption#key}
	 *
	 * @return all Configuration options grouped by the {@link ConfigurationOption#key}
	 */
	public Map<String, ConfigurationOption<?>> getConfigurationOptionsByKey() {
		return Collections.unmodifiableMap(configurationOptionsByKey);
	}

	/**
	 * Returns a map with the names of all configuration sources as key and a boolean indicating whether the configuration
	 * source supports saving as value
	 *
	 * @return the names of all configuration sources
	 */
	public Map<String, Boolean> getNamesOfConfigurationSources() {
		final Map<String, Boolean> result = new LinkedHashMap<String, Boolean>();
		for (ConfigurationSource configurationSource : configurationSources) {
			result.put(configurationSource.getName(), configurationSource.isSavingPossible());
		}
		return result;
	}

	/**
	 * Returns a configuration option by its {@link ConfigurationOption#key}
	 *
	 * @param key the configuration key
	 * @return the configuration option with a specific key
	 */
	public ConfigurationOption<?> getConfigurationOptionByKey(String key) {
		return configurationOptionsByKey.get(key);
	}

	/**
	 * Schedules {@link #reloadDynamicConfigurationOptions()} at a fixed rate
	 *
	 * @param rate     the period between reloads
	 * @param timeUnit the time unit of rate
	 */
	public void scheduleReloadAtRate(final long rate, TimeUnit timeUnit) {
		configurationReloader.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				logger.debug("Beginning scheduled configuration reload (interval is {} sec)...", rate);
				reloadDynamicConfigurationOptions();
				logger.debug("Finished scheduled configuration reload");
			}
		}, rate, rate, timeUnit);
	}

	/**
	 * Reloads a specific dynamic configuration option.
	 *
	 * @param key the key of the configuration option
	 */
	public void reload(String key) {
		if (configurationOptionsByKey.containsKey(key)) {
			configurationOptionsByKey.get(key).reload(false);
		}
	}

	/**
	 * This method reloads all configuration options - even non {@link ConfigurationOption#dynamic} ones.
	 * <p/>
	 * Use this method judiciously, because you have to make sure that no one already read from a non dynamic
	 * {@link ConfigurationOption} before calling this method.
	 */
	public void reloadAllConfigurationOptions() {
		reload(true);
	}

	/**
	 * Reloads all {@link ConfigurationOption}s where {@link ConfigurationOption#dynamic} is true
	 */
	public void reloadDynamicConfigurationOptions() {
		reload(false);
	}

	private void reload(final boolean reloadNonDynamicValues) {
		for (ConfigurationSource configurationSource : configurationSources) {
			try {
				configurationSource.reload();
			} catch (Exception e) {
				logger.warn(e.getMessage() + " (this exception is ignored)", e);
			}
		}
		for (ConfigurationOption<?> configurationOption : configurationOptionsByKey.values()) {
			configurationOption.reload(reloadNonDynamicValues);
		}
	}

	/**
	 * Adds a configuration source as first priority to the configuration.
	 * <p/>
	 * Don't forget to call {@link #reloadAllConfigurationOptions()} or {@link #reloadDynamicConfigurationOptions()}
	 * after adding all configuration sources.
	 *
	 * @param configurationSource the configuration source to add
	 */
	public void addConfigurationSource(ConfigurationSource configurationSource) {
		addConfigurationSource(configurationSource, true);
	}

	/**
	 * Adds a configuration source to the configuration.
	 * <p/>
	 * Don't forget to call {@link #reloadAllConfigurationOptions()} or {@link #reloadDynamicConfigurationOptions()}
	 * after adding all configuration sources.
	 *
	 * @param configurationSource the configuration source to add
	 * @param firstPrio           whether the configuration source should be first or last priority
	 */
	public void addConfigurationSource(ConfigurationSource configurationSource, boolean firstPrio) {
		if (configurationSource == null) {
			return;
		}
		if (firstPrio) {
			configurationSources.add(0, configurationSource);
		} else {
			configurationSources.add(configurationSource);
		}
	}

	/**
	 * Returns <code>true</code>, if the password that is required to {@link #save(String, String, String, String)} settings is
	 * set (not <code>null</code>), <code>false</code> otherwise
	 *
	 * @return <code>true</code>, if the update configuration password is set, <code>false</code> otherwise
	 */
	public boolean isPasswordSet() {
		return getString(updateConfigPasswordKey) != null;
	}

	/**
	 * Dynamically updates a configuration key.
	 * <p/>
	 * Performs a password check.
	 *
	 * @param key                         the configuration key
	 * @param value                       the configuration value
	 * @param configurationSourceName     the {@link org.stagemonitor.core.configuration.source.ConfigurationSource#getName()}
	 *                                    of the configuration source the value should be stored to
	 * @param configurationUpdatePassword the password (must not be null)
	 * @throws IOException                   if there was an error saving the key to the source
	 * @throws IllegalStateException         if the update configuration password did not match
	 * @throws IllegalArgumentException      if there was a error processing the configuration key or value or the
	 *                                       configurationSourceName did not match any of the available configuration
	 *                                       sources
	 * @throws UnsupportedOperationException if saving values is not possible with this configuration source
	 */
	public void save(String key, String value, String configurationSourceName, String configurationUpdatePassword) throws IOException,
			IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
		assertPasswordCorrect(configurationUpdatePassword);
		final ConfigurationOption<?> configurationOption = validateConfigurationOption(key, value);
		saveToConfigurationSource(key, value, configurationSourceName, configurationOption);
	}

	/**
	 * Validates a password.
	 *
	 * @param password the provided password to validate
	 * @return <code>true</code>, if the password is correct, <code>false</code> otherwise
	 */
	public boolean isPasswordCorrect(String password) {
		final String actualPassword = getString(updateConfigPasswordKey);
		return "".equals(actualPassword) || actualPassword != null && actualPassword.equals(password);
	}

	/**
	 * Validates a password. If not valid, throws a {@link IllegalStateException}.
	 *
	 * @param password the provided password to validate
	 * @throws IllegalStateException if the password did not match
	 */
	public void assertPasswordCorrect(String password) {
		if (!isPasswordSet()) {
			throw new IllegalStateException("'" + updateConfigPasswordKey + "' is not set.");
		}

		if (!isPasswordCorrect(password)) {
			throw new IllegalStateException("Wrong password for '" + updateConfigPasswordKey + "'.");
		}
	}

	/**
	 * Dynamically updates a configuration key.
	 * <p/>
	 * Does not perform a password check.
	 *
	 * @param key                         the configuration key
	 * @param value                       the configuration value
	 * @param configurationSourceName     the {@link org.stagemonitor.core.configuration.source.ConfigurationSource#getName()}
	 *                                    of the configuration source the value should be stored to
	 * @throws IOException                   if there was an error saving the key to the source
	 * @throws IllegalArgumentException      if there was a error processing the configuration key or value or the
	 *                                       configurationSourceName did not match any of the available configuration
	 *                                       sources
	 * @throws UnsupportedOperationException if saving values is not possible with this configuration source
	 */
	public void save(String key, String value, String configurationSourceName) throws IOException {
		final ConfigurationOption<?> configurationOption = validateConfigurationOption(key, value);
		saveToConfigurationSource(key, value, configurationSourceName, configurationOption);
	}

	private ConfigurationOption<?> validateConfigurationOption(String key, String value) throws IllegalArgumentException {
		final ConfigurationOption configurationOption = getConfigurationOptionByKey(key);
		if (configurationOption != null) {
			configurationOption.assertValid(value);
			return configurationOption;
		} else {
			throw new IllegalArgumentException("Config key '" + key + "' does not exist.");
		}
	}

	private void saveToConfigurationSource(String key, String value, String configurationSourceName, ConfigurationOption<?> configurationOption) throws IOException {
		for (ConfigurationSource configurationSource : configurationSources) {
			if (configurationSourceName != null && configurationSourceName.equals(configurationSource.getName())) {
				validateConfigurationSource(configurationSource, configurationOption);
				configurationSource.save(key, value);
				reload(key);
				logger.info("Updated configuration: {}={} ({})", key, value, configurationSourceName);
				return;
			}
		}
		throw new IllegalArgumentException("Configuration source '" + configurationSourceName + "' does not exist.");
	}

	private void validateConfigurationSource(ConfigurationSource configurationSource, ConfigurationOption<?> configurationOption) {
		if (!configurationOption.isDynamic() && !configurationSource.isSavingPersistent()) {
			throw new IllegalArgumentException("Non dynamic options can't be saved to a transient configuration source.");
		}
	}

	private String getString(String key) {
		if (key == null || key.isEmpty()) {
			return null;
		}
		String property = null;
		for (ConfigurationSource configurationSource : configurationSources) {
			property = configurationSource.getValue(key);
			if (property != null) {
				break;
			}
		}
		return property;
	}

	/**
	 * Shuts down the internal thread pool
	 */
	public void close() {
		configurationReloader.shutdown();
	}

	boolean isFailOnMissingRequiredValues() {
		return failOnMissingRequiredValues;
	}

}
