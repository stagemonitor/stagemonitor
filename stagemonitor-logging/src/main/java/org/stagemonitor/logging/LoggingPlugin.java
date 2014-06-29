package org.stagemonitor.logging;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.rest.RestClient;

public class LoggingPlugin implements StageMonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		RestClient.sendGrafanaDashboardAsync(configuration.getElasticsearchUrl(), "Logging.json");
	}
}
