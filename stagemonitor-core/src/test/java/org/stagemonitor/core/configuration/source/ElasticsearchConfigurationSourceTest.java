package org.stagemonitor.core.configuration.source;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElasticsearchConfigurationSourceTest extends AbstractElasticsearchTest {

	private ElasticsearchConfigurationSource configurationSource;

	@BeforeClass
	public static void setup() throws Exception {
		new CorePlugin().initializePlugin(new MetricRegistry(), Stagemonitor.getConfiguration());
		new CorePlugin().initializePlugin(new MetricRegistry(), Stagemonitor.getConfiguration());
		// give the async tasks time to complete
		Thread.sleep(500);
		refresh();
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
		assertEquals("bar", configurationSource.getValue("foo"));
	}

	@Test
	public void testGetName() throws Exception {
		assertEquals("Elasticsearch (test)", configurationSource.getName());
	}

	@Test
	public void testIsSavingPersistent() throws Exception {
		assertTrue(configurationSource.isSavingPersistent());
	}

	@Test
	public void testIsSavingPossible() throws Exception {
		assertTrue(configurationSource.isSavingPossible());
	}

	@Test
	public void testMapping() throws Exception {
		InputStream resourceAsStream = getClass().getClassLoader()
				.getResourceAsStream("stagemonitor-elasticsearch-mapping.json");
		ElasticsearchClient.sendAsJson("PUT", "/stagemonitor", resourceAsStream);
		refresh();
		configurationSource.save("foo", "bar");
		refresh();

		final GetMappingsResponse mappings = client.admin().indices().prepareGetMappings("stagemonitor").setTypes("configuration").get();
		assertEquals(1, mappings.getMappings().size());
		assertEquals("{\"configuration\":" +
				"{\"dynamic_templates\":[{\"fields\":{\"mapping\":{\"index\":\"not_analyzed\",\"type\":\"string\"},\"match\":\"*\"}}]," +
				"\"_all\":{\"enabled\":false}," +
				"\"properties\":{\"foo\":{\"type\":\"string\",\"index\":\"not_analyzed\"}}}}",
				mappings.getMappings().get("stagemonitor").get("configuration").source().toString());
	}
}
