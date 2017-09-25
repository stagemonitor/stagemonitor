package org.stagemonitor.core.util;

import org.stagemonitor.configuration.source.PropertyFileConfigurationSource;

import java.util.Properties;

public final class VersionUtils {

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

	public static String getMavenCentralDownloadLink(String groupId, String artifactId, String version) {
		return String.format("http://central.maven.org/maven2/%s/%s/%s/%s-%s.jar", groupId.replace('.', '/'), artifactId, version, artifactId, version);
	}
}
