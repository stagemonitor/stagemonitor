package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;

public class TestPlugin implements StageMonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
	}

}
