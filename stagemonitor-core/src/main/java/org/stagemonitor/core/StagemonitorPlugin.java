package org.stagemonitor.core;

import java.util.Collections;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;

/**
 * Can be used for stagemonitor Plugins. The {@link #initializePlugin(MetricRegistry, org.stagemonitor.core.configuration.Configuration)} )} Method serves as a initialize callback
 * for plugins that are not invoked by the application otherwise.
 */
public abstract class StagemonitorPlugin implements ConfigurationOptionProvider {

	/**
	 * Implementing classes have to initialize the plugin by registering their metrics the
	 * {@link com.codahale.metrics.MetricRegistry}
	 */
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {
	}

	public void onShutDown() {
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Collections.emptyList();
	}
}
