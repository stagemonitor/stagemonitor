package org.stagemonitor.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchLoggingAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {
	private final Logger logger = LoggerFactory.getLogger(ElasticsearchLoggingAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		logger.debug("sending grafana ElasticsearchLogging...");
		elasticsearchClient.sendMetricDashboardBulkAsync("kibana/Logging.bulk");
		logger.debug("sent grafana ElasticsearchLogging...");
	}

}
