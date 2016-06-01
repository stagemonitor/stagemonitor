package org.stagemonitor.core.grafana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;

public class GrafanaClientTest {

	private GrafanaClient grafanaClient;
	private HttpClient httpClient;

	@Before
	public void setUp() throws Exception {
		httpClient = mock(HttpClient.class);
		CorePlugin corePlugin = mock(CorePlugin.class);
		when(corePlugin.getThreadPoolQueueCapacityLimit()).thenReturn(1000);
		when(corePlugin.getGrafanaUrl()).thenReturn("foo");
		when(corePlugin.getGrafanaApiKey()).thenReturn("bar");
		when(corePlugin.getElasticsearchReportingInterval()).thenReturn(60);
		grafanaClient = new GrafanaClient(corePlugin, httpClient);

	}

	@Test
	public void sendGrafanaDashboardAsync() throws Exception {
		grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchCustomMetricsDashboard.json");
		grafanaClient.waitForCompletion();
		ArgumentCaptor<String> dashboardJson = ArgumentCaptor.forClass(String.class);
		verify(httpClient).sendAsJson(any(), any(), dashboardJson.capture(), any());

		final JsonNode dashboard = JsonUtils.getMapper().readTree(dashboardJson.getValue());
		boolean intervalFound = false;
		for (JsonNode template : dashboard.get("dashboard").get("templating").get("list")) {
			if ("Interval".equals(template.get("name").textValue())) {
				intervalFound = true;
				assertEquals("60s", template.get("auto_min").textValue());
			}
		}
		assertTrue(intervalFound);
	}

}