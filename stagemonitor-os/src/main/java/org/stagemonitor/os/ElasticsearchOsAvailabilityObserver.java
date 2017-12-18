package org.stagemonitor.os;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;

public class ElasticsearchOsAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {
	private final Logger logger = LoggerFactory.getLogger(ElasticsearchOsAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable() {
		logger.info("sending grafana ElasticsearchHostDashboard...");
		grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchHostDashboard.json");
		elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Host.bulk", true);
		logger.info("sent grafana ElasticsearchHostDashboard...");
	}
}
