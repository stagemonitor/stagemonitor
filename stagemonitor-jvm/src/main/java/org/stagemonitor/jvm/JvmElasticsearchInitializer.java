package org.stagemonitor.jvm;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchInitializer;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class JvmElasticsearchInitializer extends AbstractElasticsearchInitializer {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync(elasticsearchClient.getElasticsearchResourcePath() + "JVM.bulk");
	}

}
