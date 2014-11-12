package org.stagemonitor.logging;

import java.util.Collections;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class LoggingPlugin implements StagemonitorPlugin {

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		ElasticsearchClient.sendGrafanaDashboardAsync("Logging.json");
	}
}
