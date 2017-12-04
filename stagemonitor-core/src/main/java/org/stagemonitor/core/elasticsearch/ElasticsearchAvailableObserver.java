package org.stagemonitor.core.elasticsearch;

import org.stagemonitor.configuration.ConfigurationRegistry;

public interface ElasticsearchAvailableObserver {

	void init(ConfigurationRegistry configurationRegistry);

	void onElasticsearchAvailable();

}
