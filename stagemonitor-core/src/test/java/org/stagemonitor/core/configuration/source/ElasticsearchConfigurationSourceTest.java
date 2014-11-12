package org.stagemonitor.core.configuration.source;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;

public class ElasticsearchConfigurationSourceTest extends AbstractElasticsearchTest {

	private ElasticsearchConfigurationSource configurationSource;

	@BeforeClass
	public static void setup() throws Exception {
		new CorePlugin().initializePlugin(null, null);
		Thread.sleep(100);
	}

	@Before
	public void setUp() throws Exception {
		configurationSource = new ElasticsearchConfigurationSource("test");
	}

	@Test
	public void testSaveAndGet() throws Exception {
		configurationSource.save("foo", "bar");
		refresh();
		configurationSource.reload();
		Assert.assertEquals("bar", configurationSource.getValue("foo"));
	}

	@Test
	public void testGetName() throws Exception {
		Assert.assertEquals("Elasticsearch (test)", configurationSource.getName());
	}

	@Test
	public void testIsSavingPersistent() throws Exception {
		Assert.assertTrue(configurationSource.isSavingPersistent());
	}

	@Test
	public void testIsSavingPossible() throws Exception {
		Assert.assertTrue(configurationSource.isSavingPossible());
	}
}
