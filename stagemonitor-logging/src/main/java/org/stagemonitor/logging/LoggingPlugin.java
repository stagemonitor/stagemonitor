package org.stagemonitor.logging;

import java.util.Collections;
import java.util.List;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

public class LoggingPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(Metric2Registry metricRegistry, Configuration configuration) {
		ElasticsearchClient elasticsearchClient = configuration.getConfig(CorePlugin.class).getElasticsearchClient();
		elasticsearchClient.sendGrafana1DashboardAsync("Logging.json");
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.singletonList("/stagemonitor/static/tabs/metrics/logging-metrics");
	}

}
