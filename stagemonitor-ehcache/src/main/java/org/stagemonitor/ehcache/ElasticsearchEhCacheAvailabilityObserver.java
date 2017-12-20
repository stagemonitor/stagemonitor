package org.stagemonitor.ehcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchEhCacheAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchEhCacheAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		logger.info("sending grafana ElasticsearchEhCahce...");
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchEhCache.json");
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/EhCache.bulk", true);
		logger.info("sent grafana ElasticsearchEhCahce...");
	}
	@Override
	public int getPriority() {
		return 0;
	}
}

