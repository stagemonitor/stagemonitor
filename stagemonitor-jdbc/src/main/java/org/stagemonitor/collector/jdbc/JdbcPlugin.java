package org.stagemonitor.collector.jdbc;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorPlugin;
import org.stagemonitor.collector.core.rest.RestClient;

public class JdbcPlugin implements StageMonitorPlugin {
	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration config) {
		RestClient.sendGrafanaDashboardAsync(config.getElasticsearchUrl(), "JDBC Connections.json");
	}
}
