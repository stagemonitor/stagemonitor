package org.stagemonitor.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.source.PropertyFileConfigurationSource;

import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

public final class VersionUtils {

	private static final Logger logger = LoggerFactory.getLogger(VersionUtils.class);

	private VersionUtils() {
	}

	public static String getVersionFromPomProperties(Class clazz, String groupId, String artifactId) {
		final String classpathLocation = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
		final Properties pomProperties = PropertyFileConfigurationSource.getFromClasspath(classpathLocation, clazz.getClassLoader());
		if (pomProperties != null) {
			return pomProperties.getProperty("version");
		}
		return null;
	}

	public static Integer getMajorVersionFromPomProperties(Class clazz, String groupId, String artifactId) {
		String version = getVersionFromPomProperties(clazz, groupId, artifactId);
		return getMajorVersion(version);
	}

	public static Integer getMajorVersion(String version) {
		if (version != null) {
			StringTokenizer stringTokenizer = new StringTokenizer(version, ".");
			try {
				Integer majorVersion = Integer.valueOf(stringTokenizer.nextToken());
				logger.debug(String.format("Parsed version: %s, got major version: %d ", version, majorVersion));
				return majorVersion;
			} catch (NumberFormatException nfe) {
				logger.error(nfe.getMessage(), nfe);
			} catch (NoSuchElementException nsee) {
				logger.error(nsee.getMessage(), nsee);
			}
		}
		return null;
	}

	public static String getMavenCentralDownloadLink(String groupId, String artifactId, String version) {
		return String.format("http://central.maven.org/maven2/%s/%s/%s/%s-%s.jar", groupId.replace('.', '/'), artifactId, version, artifactId, version);
	}

}
