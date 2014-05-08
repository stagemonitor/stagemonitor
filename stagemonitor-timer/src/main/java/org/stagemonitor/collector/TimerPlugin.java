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
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		final String serverUrl = configuration.getServerUrl();
		if (serverUrl != null && !serverUrl.isEmpty()) {
			try {
				addElasticsearchMapping(serverUrl);
				RestClient.sendAsJsonAsync(serverUrl + "/kibana-int/dashboard/Recent%20Requests", "PUT",
						getClass().getClassLoader().getResourceAsStream("Recent Requests.json"));
			} catch (RuntimeException e) {
				logger.warn("Error while sending data to elasticsearch. Is the server URL (" + serverUrl + ") correct?"
						+ " (this exception is ignored)", e);
			}
		}
	}

	private void addElasticsearchMapping(String serverUrl) {
		final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("stagemonitor-elasticsearch-index-template.json");
		RestClient.sendAsJson(serverUrl + "/_template/stagemonitor", "PUT", resourceAsStream);

	}

}
