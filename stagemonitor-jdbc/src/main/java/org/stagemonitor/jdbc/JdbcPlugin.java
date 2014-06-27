package org.stagemonitor.jdbc;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.rest.RestClient;

public class JdbcPlugin implements StageMonitorPlugin {
	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "JDBC Connections.json");
	}
}
