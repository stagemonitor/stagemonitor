package org.stagemonitor.core.elasticsearch;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.StagemonitorSPI;

public interface ElasticsearchAvailabilityObserver extends StagemonitorSPI {

	void init(ConfigurationRegistry configurationRegistry);

	/**
	 * When Elasticsearch is available again, all registered {@link ElasticsearchAvailabilityObserver}s will be
	 * executed. Only after that, {@link ElasticsearchClient#isElasticsearchAvailable()} will return <code>true</code>.
	 */
	void onElasticsearchAvailable(ElasticsearchClient elasticsearchClient);

	/**
	 * Higher priority observers will be executed first
	 *
	 * @return the priority of the observer
	 */
	int getPriority();

}
