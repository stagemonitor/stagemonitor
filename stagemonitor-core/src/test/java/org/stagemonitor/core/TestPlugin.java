package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

import java.util.Collections;
import java.util.List;

public class TestPlugin extends StagemonitorPlugin {

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
	}

}
