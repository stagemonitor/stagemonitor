package org.stagemonitor.logging;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;
import org.stagemonitor.core.StageMonitorPlugin;
import org.stagemonitor.core.rest.RestClient;

import java.util.Collections;
import java.util.List;

public class LoggingPlugin implements StageMonitorPlugin {

	@Override
	public List<ConfigurationOption> getConfigurationOptions() {
		return Collections.emptyList();
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		RestClient.sendGrafanaDashboardAsync(configuration.getElasticsearchUrl(), "Logging.json");
	}
}
