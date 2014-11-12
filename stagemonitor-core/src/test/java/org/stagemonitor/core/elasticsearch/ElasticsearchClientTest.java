package org.stagemonitor.core.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.stagemonitor.core.configuration.AbstractElasticsearchTest;

import static org.junit.Assert.assertEquals;

public class ElasticsearchClientTest extends AbstractElasticsearchTest {

	@Test
	public void testSendDashboard() throws Exception {
		ElasticsearchClient.sendGrafanaDashboardAsync("Test Dashboard.json").get();
		refresh();
		final JsonNode dashboard = ElasticsearchClient.getJson("/grafana-dash/dashboard/test-title");
		assertEquals("test-title", dashboard.get("_id").textValue());
	}

	@Test
	public void testGetDashboardForElasticsearch() throws Exception {
	 	String expected = "{\"user\":\"guest\",\"group\":\"guest\",\"title\":\"Test Title\",\"tags\":[\"jdbc\",\"db\"],\"dashboard\":\"{\\\"title\\\":\\\"Test Title\\\",\\\"editable\\\":true,\\\"failover\\\":false,\\\"panel_hints\\\":true,\\\"style\\\":\\\"dark\\\",\\\"refresh\\\":\\\"1m\\\",\\\"tags\\\":[\\\"jdbc\\\",\\\"db\\\"],\\\"timezone\\\":\\\"browser\\\"}\"}";
		assertEquals(expected, ElasticsearchClient.getDashboardForElasticsearch("Test Dashboard.json").toString());
	}

	@Test
	public void testGetMajorMinorVersionFromFullVersionString() {
		assertEquals("123.456", ElasticsearchClient.getMajorMinorVersionFromFullVersionString("123.456.789-SNAPSHOT"));
	}
}
