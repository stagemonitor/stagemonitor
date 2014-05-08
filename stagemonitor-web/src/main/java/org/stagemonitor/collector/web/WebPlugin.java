package org.stagemonitor.collector.web;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorPlugin;
import org.stagemonitor.collector.core.rest.RestClient;

public class WebPlugin implements StageMonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		RestClient.sendAsJsonAsync(configuration.getServerUrl(), "/grafana-dash/dashboard/Request/_create", "PUT",
				getClass().getClassLoader().getResourceAsStream("Request.json"));
	}
}
