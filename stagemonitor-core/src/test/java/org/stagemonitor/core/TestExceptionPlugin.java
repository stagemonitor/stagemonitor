package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

import java.util.Collections;
import java.util.List;

public class TestExceptionPlugin implements StagemonitorPlugin {

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
		throw new RuntimeException("This is a expected test exception. It is thrown to test whether Stagemonitor can cope with plugins that throw a exception.");
	}

}
