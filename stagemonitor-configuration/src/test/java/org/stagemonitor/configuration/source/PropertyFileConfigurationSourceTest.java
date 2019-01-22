package org.stagemonitor.configuration.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class PropertyFileConfigurationSourceTest {

	@Test
	public void testLoadFromClasspath() throws Exception {
		PropertyFileConfigurationSource source = new PropertyFileConfigurationSource("test.properties");
		assertEquals("bar", source.getValue("foo"));
	}

	@Test
	public void testLoadFromJar() throws Exception {
		PropertyFileConfigurationSource source = new PropertyFileConfigurationSource("META-INF/maven/org.slf4j/slf4j-api/pom.properties");
		assertNotNull(source.getValue("version"));
		assertFalse(source.isSavingPossible());
	}

	@Test(expected = IOException.class)
	public void testSaveToJar() throws Exception {
		PropertyFileConfigurationSource source = new PropertyFileConfigurationSource("META-INF/maven/org.slf4j/slf4j-api/pom.properties");
		source.save("foo", "bar");
	}

	//@Test
	public void testLoadFromFileSystem() throws Exception {
		File properties = File.createTempFile("filesystem-test", ".properties");
		properties.deleteOnExit();
		PropertyFileConfigurationSource source = new PropertyFileConfigurationSource(properties.getAbsolutePath());
		source.save("foo2", "bar2");
		assertEquals("bar2", source.getValue("foo2"));
		assertTrue(source.isSavingPossible());
	}
}
