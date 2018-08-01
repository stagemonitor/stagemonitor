package org.stagemonitor.tracing;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchInitializer;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class TracingMetricsElasticsearchInitializer extends AbstractElasticsearchInitializer {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync(elasticsearchClient.getKibanaResourcePath() + "Request-Metrics.bulk");
	}

}
