package org.stagemonitor.collector;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorPlugin;
import org.stagemonitor.collector.core.rest.RestClient;

import java.io.InputStream;

public class TimerPlugin implements StageMonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		addElasticsearchMapping(config.getServerUrl());
		RestClient.sendGrafanaDashboardAsync(config.getServerUrl(), "Request.json");
		RestClient.sendKibanaDashboardAsync(config.getServerUrl(), "Recent Requests.json");
	}

	private void addElasticsearchMapping(String serverUrl) {
		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("stagemonitor-elasticsearch-index-template.json");
		// Sending non-asynchronously to avoid race conditions
		RestClient.sendAsJson(serverUrl, "/_template/stagemonitor", "PUT", resourceAsStream);
	}

}
