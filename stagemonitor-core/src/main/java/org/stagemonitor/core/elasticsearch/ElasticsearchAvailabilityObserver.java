package org.stagemonitor.core.elasticsearch;

import org.stagemonitor.configuration.ConfigurationRegistry;

public interface ElasticsearchAvailabilityObserver {

	void init(ConfigurationRegistry configurationRegistry);

	void onElasticsearchAvailable();

	/**
	 * Higher priority observers will be executed first
	 *
	 * @return the priority of the observer
	 */
	int getPriority();

}
