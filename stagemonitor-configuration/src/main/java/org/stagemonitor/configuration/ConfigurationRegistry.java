package org.stagemonitor.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.source.ConfigurationSource;
import org.stagemonitor.util.CollectionUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ConfigurationRegistry implements Closeable {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final List<ConfigurationSource> configurationSources = new CopyOnWriteArrayList<ConfigurationSource>();
	private final boolean failOnMissingRequiredValues;

	private Map<Class<? extends ConfigurationOptionProvider>, ConfigurationOptionProvider> optionProvidersByClass = new HashMap<Class<? extends ConfigurationOptionProvider>, ConfigurationOptionProvider>();
	private Map<String, ConfigurationOption<?>> configurationOptionsByKey = new LinkedHashMap<String, ConfigurationOption<?>>();
	private Map<String, List<ConfigurationOption<?>>> configurationOptionsByCategory = new LinkedHashMap<String, List<ConfigurationOption<?>>>();
	private ScheduledExecutorService configurationReloader;

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * @param optionProviders         the option providers
	 * @param configSources           the configuration sources
	 * @deprecated use {@link #builder()}
	 */
	@Deprecated
	public ConfigurationRegistry(Iterable<? extends ConfigurationOptionProvider> optionProviders,
								 List<ConfigurationSource> configSources) {
		this(optionProviders, configSources, false);
	}

	/**
	 * @param optionProviders         the option providers
	 * @param configSources           the configuration sources
	 * @deprecated use {@link #builder()}
	 */
	@Deprecated
	public ConfigurationRegistry(Iterable<? extends ConfigurationOptionProvider> optionProviders,
								 List<ConfigurationSource> configSources, boolean failOnMissingRequiredValues) {
		this.failOnMissingRequiredValues = failOnMissingRequiredValues;
		configurationSources.addAll(configSources);
		registerConfigurationOptions(optionProviders);
	}

	public static class Builder {

		private List<ConfigurationOptionProvider> optionProviders = new ArrayList<ConfigurationOptionProvider>();
		private List<ConfigurationSource> configSources = new ArrayList<ConfigurationSource>();
		private boolean failOnMissingRequiredValues = false;

		/**
		 * Adds a {@link ConfigurationOptionProvider}
		 *
		 * @param optionProvider the {@link ConfigurationOptionProvider} to add
		 * @return <code>this</code>, for chaining
		 */
		public Builder addOptionProvider(ConfigurationOptionProvider optionProvider) {
			this.optionProviders.add(optionProvider);
			return this;
		}

		/**
		 * Adds multiple {@link ConfigurationOptionProvider}s
		 *
		 * @param optionProviders the {@link ConfigurationOptionProvider}s to add
		 * @return <code>this</code>, for chaining
		 */
		public Builder optionProviders(Iterable<? extends ConfigurationOptionProvider> optionProviders) {
			for (ConfigurationOptionProvider optionProvider : optionProviders) {
				this.optionProviders.add(optionProvider);
			}
			return this;
		}

		/**
		 * Adds a single {@link ConfigurationSource}
		 * <p>
		 * The first configuration source which is added will have the highest precedence
		 * </p>
		 *
		 * @param configurationSource the {@link ConfigurationSource} to add
		 * @return <code>this</code>, for chaining
		 */
		public Builder addConfigSource(ConfigurationSource configurationSource) {
			this.configSources.add(configurationSource);
			return this;
		}

		/**
		 * Adds multiple {@link ConfigurationSource}s
		 * <p>
		 * The first configuration source in the provided list will have the highest precedence
		 * </p>
		 *
		 * @param configSources the {@link ConfigurationSource}s to add
		 * @return <code>this</code>, for chaining
		 */
		public Builder configSources(List<? extends ConfigurationSource> configSources) {
			this.configSources.addAll(configSources);
			return this;
		}

		/**
		 * When set to true, {@link #build()} will fail with an {@link IllegalStateException} if there are any
		 * {@link ConfigurationOption}s created with {@link ConfigurationOption.ConfigurationOptionBuilder#buildRequired()}
		 * which don't have a value set
		 *
		 * @param failOnMissingRequiredValues whether an unset but required configuration option should result in an {@link IllegalStateException}
		 * @return <code>this</code>, for chaining
		 */
		public Builder failOnMissingRequiredValues(boolean failOnMissingRequiredValues) {
			this.failOnMissingRequiredValues = failOnMissingRequiredValues;
			return this;
		}

		/**
		 * Builds a {@link ConfigurationRegistry} configured by this builder
		 *
		 * @return the {@link ConfigurationRegistry} configured by this builder
		 * @throws IllegalStateException if there are unset required configuration options and
		 *                               {@link #failOnMissingRequiredValues(boolean)} has been called
		 */
		public ConfigurationRegistry build() {
			return new ConfigurationRegistry(optionProviders, configSources, failOnMissingRequiredValues);
		}
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
		initThreadPool();
		configurationReloader.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				logger.debug("Beginning scheduled configuration reload (interval is {} sec)...", rate);
				reloadDynamicConfigurationOptions();
				logger.debug("Finished scheduled configuration reload");
			}
		}, rate, rate, timeUnit);
	}

	private void initThreadPool() {
		synchronized (this) {
			if (configurationReloader == null) {
				configurationReloader = Executors.newScheduledThreadPool(1, new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						Thread thread = new Thread(r);
						thread.setDaemon(true);
						thread.setName("configuration-reloader");
						return thread;
					}
				});
			}
		}
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
	 * <p>
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
	 * <p>
	 * Don't forget to call {@link #reloadAllConfigurationOptions()} or {@link #reloadDynamicConfigurationOptions()}
	 * after adding all configuration sources.
	 *
	 * @param configurationSource the configuration source to add
	 */
	public void addConfigurationSource(ConfigurationSource configurationSource) {
		addConfigurationSource(configurationSource, true);
	}

	public void addConfigurationSourceAfter(ConfigurationSource configurationSource, Class<? extends ConfigurationSource> addAfter) {
		if (configurationSource == null) {
			return;
		}
		CollectionUtils.addAfter(configurationSources, addAfter, configurationSource);
	}

	/**
	 * Adds a configuration source to the configuration.
	 * <p>
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
	 * Dynamically updates a configuration key.
	 * <p>
	 * Does not perform a password check.
	 *
	 * @param key                         the configuration key
	 * @param value                       the configuration value
	 * @param configurationSourceName     the {@link ConfigurationSource#getName()}
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

	/**
	 * Gets the value of a configuration key as string
	 *
	 * @param key the configuration key
	 * @return the value of this configuration key
	 */
	public String getString(String key) {
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
	@Override
	public void close() {
		if (configurationReloader != null) {
			configurationReloader.shutdown();
		}
	}

	boolean isFailOnMissingRequiredValues() {
		return failOnMissingRequiredValues;
	}

}
