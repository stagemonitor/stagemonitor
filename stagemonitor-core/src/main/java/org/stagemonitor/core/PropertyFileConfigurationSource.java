package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyFileConfigurationSource implements ConfigurationSource {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Properties properties;

	public PropertyFileConfigurationSource() {
		loadProperties();
	}

	private void loadProperties() {
		Properties defaultProperties = getProperties("stagemonitor.properties");
		if (defaultProperties == null) {
			logger.warn("Could not find stagemonitor.properties in classpath");
			defaultProperties = new Properties();
		}
		// override values in default properties file
		final String stagemonitorPropertyOverridesLocation = System.getProperty("stagemonitor.property.overrides");
		if (stagemonitorPropertyOverridesLocation != null) {
			logger.warn("try loading of default property overrides: '" + stagemonitorPropertyOverridesLocation + "'");
			properties = getProperties(stagemonitorPropertyOverridesLocation, defaultProperties);
			if (properties == null) {
				logger.warn("Could not find " + stagemonitorPropertyOverridesLocation + " in classpath");
			}
		} else {
			properties = defaultProperties;
		}
	}

	private Properties getProperties(String classpathLocation) {
		return getProperties(classpathLocation, null);
	}

	private Properties getProperties(String classpathLocation, Properties defaultProperties) {
		if (classpathLocation == null) {
			return null;
		}
		final Properties props;
		if (defaultProperties != null) {
			props = new Properties(defaultProperties);
		} else {
			props = new Properties();
		}
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
