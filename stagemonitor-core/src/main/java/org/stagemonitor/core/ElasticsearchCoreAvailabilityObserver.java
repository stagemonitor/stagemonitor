package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.util.IOUtils;

public class ElasticsearchCoreAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchCoreAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		logger.info("sending Elasticsearch CustomMetricsDashboard...");
		grafanaClient.createElasticsearchDatasource(elasticsearchClient.getElasticsearchUrl());
		grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchCustomMetricsDashboard.json");
		logger.info("sent Elasticsearch CustomMetricsDashboard...");
		logger.info("creating KibanaIndexAndMapping...");
		logger.info("created KibanaIndexAndMapping...");

	}


	@Override
	public int getPriority() {
		return Integer.MAX_VALUE;
	}
}
