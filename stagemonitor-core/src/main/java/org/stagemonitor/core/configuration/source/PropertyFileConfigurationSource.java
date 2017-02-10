package org.stagemonitor.core.configuration.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads a properties file from classpath. Falls back to loading from file system.
 */
public final class PropertyFileConfigurationSource extends AbstractConfigurationSource {

	private static final Logger logger = LoggerFactory.getLogger(PropertyFileConfigurationSource.class);

	private final String location;
	private Properties properties;
	private File file;
	private boolean writeable;

	public PropertyFileConfigurationSource(String location) {
		this.location = location;
		try {
			this.file = IOUtils.getFile(location);
			this.writeable = file.canWrite();
		} catch (Exception e) {
			this.writeable = false;
		}
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
		return writeable;
	}

	@Override
	public void save(String key, String value) throws IOException {
		if (file != null) {
			synchronized (this) {
				properties.put(key, value);
				final FileOutputStream out = new FileOutputStream(file);
				properties.store(out, null);
				out.flush();
				out.close();
			}
		} else {
			throw new IOException(location + " is not writeable");
		}
	}

}
