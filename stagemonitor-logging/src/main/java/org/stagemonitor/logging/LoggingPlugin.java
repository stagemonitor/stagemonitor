package org.stagemonitor.logging;

import java.util.Collections;
import java.util.List;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.grafana.GrafanaClient;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.IOUtils;

public class LoggingPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(Metric2Registry metricRegistry, Configuration configuration) {
		final CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		final ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		final GrafanaClient grafanaClient = corePlugin.getGrafanaClient();
		if (corePlugin.isReportToGraphite()) {
			elasticsearchClient.sendGrafana1DashboardAsync("grafana/Grafana1GraphiteLogging.json");
		}
		if (corePlugin.isReportToElasticsearch()) {
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchLogging.json");
			elasticsearchClient.sendBulk(IOUtils.getResourceAsStream("kibana/Logging.bulk"));
		}
	}

	@Override
	public List<String> getPathsOfWidgetMetricTabPlugins() {
		return Collections.singletonList("/stagemonitor/static/tabs/metrics/logging-metrics");
	}

}
