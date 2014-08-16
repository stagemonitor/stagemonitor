package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;

public class TestExceptionPlugin implements StageMonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		throw new RuntimeException("test");
	}

}
