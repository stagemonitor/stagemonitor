package org.stagemonitor.core.configuration.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a properties file from classpath. Falls back to loading from file system.
 *
 */
public final class PropertyFileConfigurationSource extends AbstractConfigurationSource {

	private static final Logger logger = LoggerFactory.getLogger(PropertyFileConfigurationSource.class);

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
	public boolean isSavingPersistent() {
		return true;
	}

	@Override
	public String getName() {
		return location;
	}

	public static boolean isPresent(String location) {
		return getProperties(location) != null;
	}

	private static Properties getProperties(String location) {
		if (location == null) {
			return null;
		}
		Properties props = getFromClasspath(location);
		if (props == null) {
			props = getFromFileSystem(location);
		}
		return props;
	}

	private static Properties getFromClasspath(String classpathLocation) {
		final Properties props = new Properties();
		InputStream resourceStream = PropertyFileConfigurationSource.class.getClassLoader().getResourceAsStream(classpathLocation);
		if (resourceStream != null) {
			try {
				props.load(resourceStream);
				logger.info("Successfully loaded {} from classpath", classpathLocation);
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

	private static Properties getFromFileSystem(String location) {
		Properties props = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(location);
			props.load(input);
			logger.info("Successfully loaded {} from file system", location);
			return props;
		} catch (FileNotFoundException ex) {
			return null;
		} catch (IOException e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
		} finally {
			if (input != null) {
				try {
					input.close();
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

	@Override
	public boolean isSavingPossible() {
		return true;
	}

	@Override
	public void save(String key, String value) throws IOException {
		synchronized (this) {
			properties.put(key, value);
			try {
				final URL resource = getClass().getClassLoader().getResource(location);
				if (resource == null) {
					throw new IOException();
				}
				File file = new File(resource.toURI());
				final FileOutputStream out = new FileOutputStream(file);
				properties.store(out, null);
				out.flush();
				out.close();
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}
	}

}
