package org.stagemonitor.core.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

public final class PropertyFileConfigurationSource extends AbstractConfigurationSource {

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
	public boolean isSavingPersistent() {
		return true;
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
