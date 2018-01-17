package org.stagemonitor.logging;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchInitializer;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class LoggingElasticsearchInitializer extends AbstractElasticsearchInitializer {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync(elasticsearchClient.getElasticsearchResourcePath() + "Logging.bulk");
	}

}
