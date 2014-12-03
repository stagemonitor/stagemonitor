package org.stagemonitor.alerting;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

import java.util.List;

public class AlertingPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {

	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return super.getConfigurationOptions();
	}
}
