package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DynamicConfigurationSource implements ConfigurationSource {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Configuration configuration;
	private final String updateConfigPasswordKey;

	private final ConcurrentMap<String, String> queryParameterConfiguration = new ConcurrentHashMap<String, String>();

	public DynamicConfigurationSource(Configuration configuration, String updateConfigPasswordKey) {
		this.configuration = configuration;
		this.updateConfigPasswordKey = updateConfigPasswordKey;
	}

	@Override
	public String getValue(String key) {
		return queryParameterConfiguration.get(key);
	}

	/**
	 * Dynamically updates a configuration key.
	 *
	 * @param key                         the configuration key (see {@link Configuration}
	 * @param value                       the configuration value
	 * @param configurationUpdatePassword the password (must not be null)
	 * @throws IllegalStateException if the update configuration password did not match
	 * @throws IllegalArgumentException if there was a error processing the configuration key or value
	 */
	public void updateConfiguration(String key, String value, String configurationUpdatePassword)
			throws IllegalArgumentException, IllegalStateException {
		String updateConfigPassword = configuration.getString(updateConfigPasswordKey);
		if (updateConfigPassword == null) {
			throw new IllegalStateException("Update configuration password is not set. " +
					"Dynamic configuration changes are therefore not allowed.");
		}

		if (updateConfigPassword.equals(configurationUpdatePassword)) {
			setNewValueIfDynamic(key, value);
		} else {
			throw new IllegalStateException("Wrong password for updating configuration.");
		}
	}

	private void setNewValueIfDynamic(String key, String value) throws IllegalArgumentException {
		final ConfigurationOption configurationOption = configuration.getConfigurationOptionByKey(key);

		if (configurationOption != null) {
			if (!configurationOption.isDynamic()) {
				throw new IllegalArgumentException("Configuration option is not dynamic.");
			}
			configurationOption.assertValid(value);
			queryParameterConfiguration.put(key, value);
			configuration.reload(key);
			logger.info("Updated configuration: {}={}", key, value);
		} else {
			throw new IllegalArgumentException("Config key '" + key + "' does not exist.");
		}
	}

	@Override
	public void reload() {
	}

	@Override
	public String getName() {
		return "Dynamic Configuration";
	}
}
