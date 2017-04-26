package org.stagemonitor.configuration.source;

import com.codahale.metrics.SharedMetricRegistries;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stagemonitor.AbstractElasticsearchTest;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.junit.ConditionalTravisTestRunner;
import org.stagemonitor.junit.ExcludeOnTravis;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(ConditionalTravisTestRunner.class)
public class ElasticsearchConfigurationSourceTest extends AbstractElasticsearchTest {

	private ElasticsearchConfigurationSource configurationSource;


	@AfterClass
	public static void reset() {
		Stagemonitor.reset();
		SharedMetricRegistries.clear();
	}

	@Before
	public void setUp() throws Exception {
		InputStream resourceAsStream = ElasticsearchConfigurationSourceTest.class.getClassLoader()
				.getResourceAsStream("stagemonitor-elasticsearch-mapping.json");
		elasticsearchClient.sendAsJson("PUT", "/stagemonitor", resourceAsStream);
		configurationSource = new ElasticsearchConfigurationSource(elasticsearchClient, "test");
	}

	@Test
	public void testSaveAndGet() throws Exception {
		configurationSource.save("foo.bar", "baz");
		refresh();
		configurationSource.reload();
		assertEquals("baz", configurationSource.getValue("foo.bar"));
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
	@ExcludeOnTravis
	public void testMapping() throws Exception {
		configurationSource.save("foo", "bar");
		refresh();

		final GetMappingsResponse mappings = client.admin().indices().prepareGetMappings("stagemonitor").setTypes("configuration").get();
		assertEquals(1, mappings.getMappings().size());
		assertEquals("{\"configuration\":{" +
						"\"_all\":{\"enabled\":false}," +
						"\"dynamic_templates\":[{\"fields\":{\"match\":\"*\",\"mapping\":{\"type\":\"keyword\"}}}]" +
						"}}",
				mappings.getMappings().get("stagemonitor").get("configuration").source().toString());
	}
}
