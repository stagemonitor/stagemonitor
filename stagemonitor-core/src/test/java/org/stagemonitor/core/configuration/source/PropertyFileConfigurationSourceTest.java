package org.stagemonitor.core.configuration.source;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;

import org.junit.Test;

public class PropertyFileConfigurationSourceTest {

	@Test
	public void testLoadFromClasspath() throws Exception {
		PropertyFileConfigurationSource propertyFileConfigurationSource = new PropertyFileConfigurationSource("test.properties");
		assertEquals("bar", propertyFileConfigurationSource.getValue("foo"));
	}

	@Test
	public void testLoadFromFileSystem() throws Exception {
		File properties = File.createTempFile("filesystem-test", ".properties");
		properties.deleteOnExit();
		new FileWriter(properties).append("foo2=bar2").close();
		PropertyFileConfigurationSource propertyFileConfigurationSource = new PropertyFileConfigurationSource(properties.getAbsolutePath());
		assertEquals("bar2", propertyFileConfigurationSource.getValue("foo2"));
	}
}
