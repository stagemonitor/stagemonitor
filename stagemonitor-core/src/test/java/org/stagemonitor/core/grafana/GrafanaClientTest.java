package org.stagemonitor.core.grafana;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Before;
import org.junit.Test;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.HttpClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GrafanaClientTest {

	private GrafanaClient grafanaClient;

	@Before
	public void setUp() throws Exception {
		CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(corePlugin.getGrafanaUrl()).thenReturn("foo");
		when(corePlugin.getGrafanaApiKey()).thenReturn("bar");
		when(corePlugin.getElasticsearchReportingInterval()).thenReturn(60);
		grafanaClient = new GrafanaClient(corePlugin, mock(HttpClient.class));
	}

	@Test
	public void sendGrafanaDashboardAsync() throws Exception {
		final JsonNode dashboard = grafanaClient.getGrafanaDashboard("grafana/ElasticsearchCustomMetricsDashboard.json");
		boolean intervalFound = false;
		for (JsonNode template : dashboard.get("templating").get("list")) {
			if ("Interval".equals(template.get("name").textValue())) {
				intervalFound = true;
				assertEquals("60s", template.get("auto_min").textValue());
			}
		}
		assertTrue(intervalFound);
	}

}
