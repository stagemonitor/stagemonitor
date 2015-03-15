package org.stagemonitor.core.elasticsearch;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;

public class ElasticsearchClientTest extends AbstractElasticsearchTest {

	@Test
	public void testSendDashboard() throws Exception {
		elasticsearchClient.sendGrafanaDashboardAsync("Test Dashboard.json").get();
		refresh();
		final JsonNode dashboard = elasticsearchClient.getJson("/grafana-dash/dashboard/test-title");
		assertEquals("test-title", dashboard.get("_id").textValue());
	}

	@Test
	public void testGetDashboardForElasticsearch() throws Exception {
	 	String expected = "{\"user\":\"guest\",\"group\":\"guest\",\"title\":\"Test Title\",\"tags\":[\"jdbc\",\"db\"],\"dashboard\":\"{\\\"title\\\":\\\"Test Title\\\",\\\"editable\\\":true,\\\"failover\\\":false,\\\"panel_hints\\\":true,\\\"style\\\":\\\"dark\\\",\\\"refresh\\\":\\\"1m\\\",\\\"tags\\\":[\\\"jdbc\\\",\\\"db\\\"],\\\"timezone\\\":\\\"browser\\\"}\"}";
		assertEquals(expected, elasticsearchClient.getDashboardForElasticsearch("Test Dashboard.json").toString());
	}

	@Test
	public void testGetMajorMinorVersionFromFullVersionString() {
		assertEquals("123.456", elasticsearchClient.getMajorMinorVersionFromFullVersionString("123.456.789-SNAPSHOT"));
	}
}
