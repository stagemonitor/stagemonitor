package org.stagemonitor.logging;

import org.stagemonitor.core.StagemonitorPlugin;

public class LoggingPlugin extends StagemonitorPlugin {

	@Override
	public void registerWidgetMetricTabPlugins(WidgetMetricTabPluginsRegistry widgetMetricTabPluginsRegistry) {
		widgetMetricTabPluginsRegistry.addWidgetMetricTabPlugin("/stagemonitor/static/tabs/metrics/logging-metrics");
	}
}
