package org.stagemonitor.logging;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.rest.RestClient;

import java.util.Collections;
import java.util.List;

public class LoggingPlugin implements StagemonitorPlugin {

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		RestClient.sendGrafanaDashboardAsync(configuration.getConfig(CorePlugin.class).getElasticsearchUrl(), "Logging.json");
	}
}
