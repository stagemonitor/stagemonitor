package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.configuration.ConfigurationRegistry;

import java.util.List;

public class ConfigurationLogger {

	private final Logger logger;

	public ConfigurationLogger() {
		logger = LoggerFactory.getLogger(ConfigurationLogger.class);
	}

	public ConfigurationLogger(Logger logger) {
		this.logger = logger;
	}

	public void logConfiguration(ConfigurationRegistry configuration) {
		logger.info("# stagemonitor configuration, listing non-default values:");
		boolean hasOnlyDefaultOptions = true;

		for (List<ConfigurationOption<?>> options : configuration.getConfigurationOptionsByCategory().values()) {
			for (ConfigurationOption<?> option : options) {
				if (!option.isDefault()) {
					hasOnlyDefaultOptions = false;
					logger.info("{}: {} (source: {})",
							option.getKey(), prepareOptionValueForLog(option), option.getNameOfCurrentConfigurationSource());
					if (option.getTags().contains("deprecated")) {
						logger.warn("Detected usage of deprecated configuration option '{}'. " +
								"This option might be removed in the future. " +
								"Please refer to the documentation about alternatives.", option.getKey());
					}

					if (!option.getKey().equals(option.getUsedKey())) {
						logger.warn("Detected usage of an old configuration key: '{}'. Please use '{}' instead.",
								option.getUsedKey(), option.getKey());
					}
				}
			}
		}

		if (hasOnlyDefaultOptions) {
			logger.warn("stagemonitor has not been configured. Have a look at " +
					"https://github.com/stagemonitor/stagemonitor/wiki/How-should-I-configure-stagemonitor%3F " +
					"and " +
					"https://github.com/stagemonitor/stagemonitor/wiki/Configuration-Options " +
					"for further instructions");
		}
	}

	private static String prepareOptionValueForLog(ConfigurationOption<?> option) {
		if (option.isSensitive()) {
			return "XXXX";
		} else {
			return option.getValueAsSafeString();
		}
	}
}
