package org.stagemonitor.web.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchServletAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchServletAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		logger.info("sending kibana Application-Server...");
		elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Application-Server.bulk", true);
		grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchApplicationServer.json");
		logger.info("sent kibana Application-Server...");
	}

	@Override
	public int getPriority() {
		return 0;
	}
}

