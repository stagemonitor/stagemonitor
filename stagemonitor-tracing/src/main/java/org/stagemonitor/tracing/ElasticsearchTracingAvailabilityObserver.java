package org.stagemonitor.tracing;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchTracingAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync(elasticsearchClient.getElasticsearchResourcePath() + "Request-Metrics.bulk");
	}

}
