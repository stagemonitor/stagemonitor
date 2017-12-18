package org.stagemonitor.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;

public class ElasticsearchTracingAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchTracingAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable() {
		logger.info("sending kibana Request-Metrics...");
		elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Request-Metrics.bulk", true);
		grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchRequestDashboard.json");
		logger.info("sent kibana Request-Metrics...");
	}

}
