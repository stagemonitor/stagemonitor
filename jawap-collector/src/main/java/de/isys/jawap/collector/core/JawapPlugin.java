package de.isys.jawap.collector.core;

import com.codahale.metrics.MetricRegistry;

/**
 * Can be used for Jawap Plugins. The {@link #initializePlugin(com.codahale.metrics.MetricRegistry)} )} Method serves as a initialize callback
 * for plugins that are not invoked by the application otherwise.
 */
public interface JawapPlugin {

	/**
	 * Implementing classes have to initialize the plugin by registering their metrics the
	 * {@link com.codahale.metrics.MetricRegistry}
	 */
	void initializePlugin(MetricRegistry metricRegistry);
}
