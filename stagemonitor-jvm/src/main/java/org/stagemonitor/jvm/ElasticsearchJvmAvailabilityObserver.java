package org.stagemonitor.jvm;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchJvmAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync(elasticsearchClient.getElasticsearchResourcePath() + "JVM.bulk");
	}

}
