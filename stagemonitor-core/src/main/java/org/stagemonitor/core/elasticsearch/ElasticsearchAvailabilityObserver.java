package org.stagemonitor.core.elasticsearch;

import org.stagemonitor.configuration.ConfigurationRegistry;

public interface ElasticsearchAvailabilityObserver {

	void init(ConfigurationRegistry configurationRegistry);

	void onElasticsearchAvailable();

}
