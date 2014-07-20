package org.stagemonitor.web.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationSource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class QueryParameterConfigurationSource implements ConfigurationSource {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Configuration configuration;

	private final ConcurrentMap<String, String> queryParameterConfiguration = new ConcurrentHashMap<String, String>();

	public QueryParameterConfigurationSource(Configuration configuration) {
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
	public void updateConfiguration(String key, String value, String configurationUpdatePassword) {
		if (configuration.getConfigurationUpdatePassword() == null) {
			logger.warn("stagemonitor.configuration.update.password is not set. " +
					"Dynamic configuration changes are therefore not allowed");
			return;
		}

		if (configuration.getConfigurationUpdatePassword().equals(configurationUpdatePassword)) {
			queryParameterConfiguration.put(key, value);
			configuration.reload();
			logger.info("Updated configuration: {}={}", key, value);
		} else {
			logger.warn("Wrong password for updating configuration");
		}
	}

	@Override
	public void reload() {
	}
}
