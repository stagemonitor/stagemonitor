package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;

import java.util.Collections;
import java.util.List;

public class TestExceptionPlugin implements StageMonitorPlugin {

	@Override
	public List<ConfigurationOption> getConfigurationOptions() {
		return Collections.emptyList();
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		throw new RuntimeException("test");
	}

}
