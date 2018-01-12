package org.stagemonitor.web.servlet;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchServletAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync(elasticsearchClient.getElasticsearchResourcePath() + "Application-Server.bulk");
	}

}

