package org.stagemonitor.web.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;
import org.stagemonitor.core.ConfigurationSource;
import org.stagemonitor.web.WebPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DynamicConfigurationSource implements ConfigurationSource {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Configuration configuration;

	private final ConcurrentMap<String, String> queryParameterConfiguration = new ConcurrentHashMap<String, String>();

	public DynamicConfigurationSource(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getValue(String key) {
		return queryParameterConfiguration.get(key);
	}

	/**
	 * Dynamically updates a configuration key.
	 *
	 * @param key the configuration key (see {@link Configuration}
	 * @param value the configuration value
	 * @param configurationUpdatePassword the password (must not be null)
	 */
	public boolean updateConfiguration(String key, String value, String configurationUpdatePassword) {
		if (configuration.getString(WebPlugin.STAGEMONITOR_PASSWORD) == null) {
			logger.warn(WebPlugin.STAGEMONITOR_PASSWORD + " is not set. " +
					"Dynamic configuration changes are therefore not allowed");
			return false;
		}

		if (configuration.getString(WebPlugin.STAGEMONITOR_PASSWORD).equals(configurationUpdatePassword)) {
			return setNewValueIfDynamic(key, value);
		} else {
			logger.warn("Wrong password for updating configuration");
			return false;
		}
	}

	private boolean setNewValueIfDynamic(String key, String value) {
		final Map<String,ConfigurationOption> configurationOptionsByKey = configuration.getConfigurationOptionsByKey();
		if (configurationOptionsByKey.containsKey(key) && configurationOptionsByKey.get(key).isDynamic()) {
			queryParameterConfiguration.put(key, value);
			configuration.reload();
			logger.info("Updated configuration: {}={}", key, value);
			return true;
		}
		return false;
	}

	@Override
	public void reload() {
	}
}
