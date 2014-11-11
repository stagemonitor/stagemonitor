package org.stagemonitor.core.configuration.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemPropertyConfigurationSource extends AbstractConfigurationSource {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public String getValue(String key) {
		try {
			return System.getProperty(key);
		} catch (SecurityException e) {
			logger.warn("Could not get Java system property, because of a SecurityException: {}", e.getMessage());
			return null;
		}
	}

	@Override
	public String getName() {
		return "Java System Properties";
	}
}
