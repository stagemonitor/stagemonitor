package org.stagemonitor.core;

import com.codahale.metrics.MetricRegistry;

/**
 * Can be used for stagemonitor Plugins. The {@link #initializePlugin(MetricRegistry, Configuration)} )} Method serves as a initialize callback
 * for plugins that are not invoked by the application otherwise.
 */
public interface StageMonitorPlugin {

	/**
	 * Implementing classes have to initialize the plugin by registering their metrics the
	 * {@link com.codahale.metrics.MetricRegistry}
	 */
	void initializePlugin(MetricRegistry metricRegistry, Configuration configuration);
}
