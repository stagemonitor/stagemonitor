package org.stagemonitor.core.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.stagemonitor.core.elasticsearch.ElasticsearchClient.requireBoxTypeHotIfHotColdAritectureActive;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;

public class ElasticsearchClientTest extends AbstractElasticsearchTest {

	@Test
	public void testSendDashboard() throws Exception {
		elasticsearchClient.sendGrafana1DashboardAsync("Test Dashboard.json").get();
		refresh();
		final JsonNode dashboard = elasticsearchClient.getJson("/grafana-dash/dashboard/test-title");
		assertEquals("test-title", dashboard.get("_id").textValue());
	}

	@Test
	public void testGetDashboardForElasticsearch() throws Exception {
		String expected = "{\"user\":\"guest\",\"group\":\"guest\",\"title\":\"Test Title\",\"tags\":[\"jdbc\",\"db\"],\"dashboard\":\"{\\\"title\\\":\\\"Test Title\\\",\\\"editable\\\":false,\\\"failover\\\":false,\\\"panel_hints\\\":true,\\\"style\\\":\\\"dark\\\",\\\"refresh\\\":\\\"1m\\\",\\\"tags\\\":[\\\"jdbc\\\",\\\"db\\\"],\\\"timezone\\\":\\\"browser\\\"}\"}";
		assertEquals(expected, elasticsearchClient.getDashboardForElasticsearch("Test Dashboard.json").toString());
	}

	@Test
	public void testRequireBoxTypeHotWhenHotColdActive() throws Exception {
		assertTrue(requireBoxTypeHotIfHotColdAritectureActive("stagemonitor-elasticsearch-metrics-index-template.json", 2).contains("hot"));
	}

	@Test
	public void testDontRequireBoxTypeHotWhenHotColdInactive() throws Exception {
		assertFalse(requireBoxTypeHotIfHotColdAritectureActive("stagemonitor-elasticsearch-metrics-index-template.json", 0).contains("hot"));
		assertFalse(requireBoxTypeHotIfHotColdAritectureActive("stagemonitor-elasticsearch-metrics-index-template.json", -1).contains("hot"));
	}

}
