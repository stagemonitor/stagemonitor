package org.stagemonitor.os;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchInitializer;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class OsElasticsearchInitializer extends AbstractElasticsearchInitializer {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync(elasticsearchClient.getKibanaResourcePath() + "Host.bulk");
	}
}
