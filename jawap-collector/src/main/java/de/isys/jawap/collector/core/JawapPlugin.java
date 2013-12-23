package de.isys.jawap.collector.core;

/**
 * Can be used for Jawap Plugins. The {@link #initializePlugin()} Method serves as a initialize callback
 * for plugins that are not invoked by the application otherwise.
 */
public interface JawapPlugin {

	/**
	 * Implementing classes have to initialize the plugin by registering their metrics in a
	 * {@link com.codahale.metrics.MetricRegistry} that is added to  the
	 * {@link com.codahale.metrics.SharedMetricRegistries}
	 */
	void initializePlugin();
}
