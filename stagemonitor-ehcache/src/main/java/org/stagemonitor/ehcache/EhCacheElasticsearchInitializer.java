package org.stagemonitor.ehcache;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchInitializer;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class EhCacheElasticsearchInitializer extends AbstractElasticsearchInitializer {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync(elasticsearchClient.getKibanaResourcePath() + "EhCache.bulk");
	}

}

