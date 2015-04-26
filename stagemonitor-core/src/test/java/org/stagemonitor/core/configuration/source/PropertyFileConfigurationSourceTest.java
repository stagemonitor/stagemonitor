package org.stagemonitor.core.configuration.source;

import static org.junit.Assert.assertEquals;

import java.io.File;

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
		PropertyFileConfigurationSource propertyFileConfigurationSource = new PropertyFileConfigurationSource(properties.getAbsolutePath());
		propertyFileConfigurationSource.save("foo2", "bar2");
		assertEquals("bar2", propertyFileConfigurationSource.getValue("foo2"));
	}
}
