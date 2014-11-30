package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.configuration.Configuration;

public class TestExceptionPlugin extends StagemonitorPlugin {

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		throw new RuntimeException("This is a expected test exception. It is thrown to test whether Stagemonitor can cope with plugins that throw a exception.");
	}

}
