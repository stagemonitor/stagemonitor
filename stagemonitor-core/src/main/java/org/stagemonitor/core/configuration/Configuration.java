package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class Configuration {


	private final Logger logger = LoggerFactory.getLogger(getClass());
	private List<ConfigurationSource> configurationSources = new LinkedList<ConfigurationSource>();

	private Map<Class<? extends ConfigurationOptionProvider>, ConfigurationOptionProvider> pluginConfiguration = new HashMap<Class<? extends ConfigurationOptionProvider>, ConfigurationOptionProvider>();
	private Map<String, ConfigurationOption> configurationOptionsByKey = new LinkedHashMap<String, ConfigurationOption>();
	private Map<String, List<ConfigurationOption>> configurationOptionsByPlugin = new LinkedHashMap<String, List<ConfigurationOption>>();

	public Configuration() {
		this(ConfigurationOptionProvider.class);
	}

	/**
	 *
	 * @param optionProviderClass the class that should be used to lookup instances of
	 * {@link ConfigurationOptionProvider} via {@link ServiceLoader#load(Class)}
	 */
	public Configuration(Class<? extends ConfigurationOptionProvider> optionProviderClass) {
		configurationSources.add(new SystemPropertyConfigurationSource());
		final String stagemonitorPropertyOverridesLocation = System.getProperty("stagemonitor.property.overrides");
		if (stagemonitorPropertyOverridesLocation != null) {
			logger.info("try loading of default property overrides: '" + stagemonitorPropertyOverridesLocation + "'");
			configurationSources.add(new PropertyFileConfigurationSource(stagemonitorPropertyOverridesLocation));
		}
		configurationSources.add(new PropertyFileConfigurationSource("stagemonitor.properties"));
		registerConfigurationOptions(optionProviderClass);
	}

	private void registerConfigurationOptions(Class<? extends ConfigurationOptionProvider> optionProviderClass) {
		for (ConfigurationOptionProvider configurationOptionProvider : ServiceLoader.load(optionProviderClass)) {
			try {
				registerPluginConfiguration(configurationOptionProvider);
			} catch (RuntimeException e) {
				logger.warn("Error while initializing configuration options for " +
						configurationOptionProvider.getClass() + " (this exception is ignored)", e);
			}
		}
	}

	private void registerPluginConfiguration(ConfigurationOptionProvider configurationOptionProvider) {
		pluginConfiguration.put(configurationOptionProvider.getClass(), configurationOptionProvider);
		for (ConfigurationOption configurationOption : configurationOptionProvider.getConfigurationOptions()) {
			add(configurationOption);
		}
	}

	public <T extends ConfigurationOptionProvider> T getConfig(Class<T> configClass) {
		return (T) pluginConfiguration.get(configClass);
	}

	private void add(final ConfigurationOption configurationOption) {
		configurationOption.setConfigurationSources(configurationSources);
		configurationOption.reload();

		configurationOptionsByKey.put(configurationOption.getKey(), configurationOption);
		addConfigurationOptionByPlugin(configurationOption.getPluginName(), configurationOption);
	}

	private void addConfigurationOptionByPlugin(String pluginName, final ConfigurationOption configurationOption) {
		if (configurationOptionsByPlugin.containsKey(pluginName)) {
			configurationOptionsByPlugin.get(pluginName).add(configurationOption);
		} else {
			configurationOptionsByPlugin.put(pluginName, new ArrayList<ConfigurationOption>() {{
				add(configurationOption);
			}});
		}
	}

	public Map<String, List<ConfigurationOption>> getConfigurationOptionsByPlugin() {
		return Collections.unmodifiableMap(configurationOptionsByPlugin);
	}

	public Map<String, ConfigurationOption> getConfigurationOptionsByKey() {
		return Collections.unmodifiableMap(configurationOptionsByKey);
	}

	public void reload(String key) {
		if (configurationOptionsByKey.containsKey(key)) {
			configurationOptionsByKey.get(key).reload();
		}
	}

	public void reload() {
		for (ConfigurationSource configurationSource : configurationSources) {
			configurationSource.reload();
		}
		for (ConfigurationOption configurationOption : configurationOptionsByKey.values()) {
			configurationOption.reload();
		}
	}

	public void addConfigurationSource(ConfigurationSource configurationSource) {
		addConfigurationSource(configurationSource, true);
	}

	public void addConfigurationSource(ConfigurationSource configurationSource, boolean firstPrio) {
		if (configurationSource == null) {
			return;
		}
		if (firstPrio) {
			configurationSources.add(0, configurationSource);
		} else {
			configurationSources.add(configurationSource);
		}
		reload();
	}


	@Deprecated
	public String getString(String key) {
		String property = null;
		for (ConfigurationSource configurationSource : configurationSources) {
			property = configurationSource.getValue(key);
			if (property != null) {
				break;
			}
		}
		if (property != null) {
			return property.trim();
		} else {
			final ConfigurationOption configurationOption = configurationOptionsByKey.get(key);
			if (configurationOption == null) {
				logger.error("Configuration option with key '{}' ist not registered!", key);
				return null;
			}
			return configurationOption.getDefaultValueAsString();
		}
	}

}
