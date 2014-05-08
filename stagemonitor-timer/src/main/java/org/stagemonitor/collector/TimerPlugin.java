package org.stagemonitor.collector;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorPlugin;
import org.stagemonitor.collector.core.rest.RestClient;

import java.io.InputStream;

public class TimerPlugin implements StageMonitorPlugin {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		addElasticsearchMapping(config.getServerUrl());
		RestClient.sendAsJsonAsync(config.getServerUrl(), "/kibana-int/dashboard/Recent%20Requests/_create", "PUT",
				getClass().getClassLoader().getResourceAsStream("Recent Requests.json"));
	}

	private void addElasticsearchMapping(String serverUrl) {
		final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("stagemonitor-elasticsearch-index-template.json");
		RestClient.sendAsJson(serverUrl + "/_template/stagemonitor", "PUT", resourceAsStream);
	}

}
