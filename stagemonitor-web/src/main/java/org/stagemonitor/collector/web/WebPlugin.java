package org.stagemonitor.collector.web;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorPlugin;
import org.stagemonitor.collector.core.rest.RestClient;

public class WebPlugin implements StageMonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		final String serverUrl = configuration.getServerUrl();
		if (serverUrl != null && !serverUrl.isEmpty()) {
			RestClient.sendAsJsonAsync(serverUrl + "/grafana-dash/dashboard/Request?version=1", "PUT",
					getClass().getClassLoader().getResourceAsStream("Request.json"));
		}
	}
}
