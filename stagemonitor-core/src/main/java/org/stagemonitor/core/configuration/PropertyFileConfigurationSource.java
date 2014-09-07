package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertyFileConfigurationSource implements ConfigurationSource {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Properties properties;

	private final String location;

	public PropertyFileConfigurationSource(String location) {
		this.location = location;
		reload();
	}

	@Override
	public void reload() {
		properties = getProperties(location);
		if (properties == null) {
			logger.warn("Could not load {}", location);
			properties = new Properties();
		}
	}

	@Override
	public String getName() {
		return location;
	}

	private Properties getProperties(String classpathLocation) {
		if (classpathLocation == null) {
			return null;
		}
		final Properties props = new Properties();
		InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(classpathLocation);
		if (resourceStream != null) {
			try {
				props.load(resourceStream);
				return props;
			} catch (IOException e) {
				logger.warn(e.getMessage() + " (this exception is ignored)", e);
			} finally {
				try {
					resourceStream.close();
				} catch (IOException e) {
					logger.warn(e.getMessage() + " (this exception is ignored)", e);
				}
			}
		}
		return null;
	}

	@Override
	public String getValue(String key) {
		return properties.getProperty(key);
	}
}
