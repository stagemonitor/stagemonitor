package org.stagemonitor.core.configuration.source;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PropertyFileConfigurationSourceTest {

	@Test
	public void testLoadFromClasspath() throws Exception {
		PropertyFileConfigurationSource propertyFileConfigurationSource = new PropertyFileConfigurationSource("test.properties");
		assertEquals("bar", propertyFileConfigurationSource.getValue("foo"));
	}

	@Test
	public void testLoadFromJar() throws Exception {
		PropertyFileConfigurationSource propertyFileConfigurationSource = new PropertyFileConfigurationSource("META-INF/maven/org.slf4j/slf4j-api/pom.properties");
		assertNotNull(propertyFileConfigurationSource.getValue("version"));
		assertFalse(propertyFileConfigurationSource.isSavingPossible());
	}

	@Test
	public void testLoadFromFileSystem() throws Exception {
		File properties = File.createTempFile("filesystem-test", ".properties");
		properties.deleteOnExit();
		PropertyFileConfigurationSource propertyFileConfigurationSource = new PropertyFileConfigurationSource(properties.getAbsolutePath());
		propertyFileConfigurationSource.save("foo2", "bar2");
		assertEquals("bar2", propertyFileConfigurationSource.getValue("foo2"));
		assertTrue(propertyFileConfigurationSource.isSavingPossible());
	}
}
