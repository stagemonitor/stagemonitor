package org.stagemonitor.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;

public class ElasticsearchEhCacheAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchEhCacheAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable() {
		logger.info("sending grafana ElasticsearchEhCahce...");
			grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchEhCache.json");
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/EhCache.bulk", true);
		logger.info("sent grafana ElasticsearchEhCahce...");
	}
}
