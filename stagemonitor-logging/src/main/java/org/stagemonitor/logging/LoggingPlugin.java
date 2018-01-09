package org.stagemonitor.logging;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;

public class LoggingPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(StagemonitorPlugin.InitArguments initArguments) {
		final CorePlugin corePlugin = initArguments.getPlugin(CorePlugin.class);
		corePlugin.getGrafanaClient().sendGrafanaDashboardAsync("grafana/ElasticsearchLogging.json");
	}

	@Override
	public void registerWidgetMetricTabPlugins(WidgetMetricTabPluginsRegistry widgetMetricTabPluginsRegistry) {
		widgetMetricTabPluginsRegistry.addWidgetMetricTabPlugin("/stagemonitor/static/tabs/metrics/logging-metrics");
	}
}
