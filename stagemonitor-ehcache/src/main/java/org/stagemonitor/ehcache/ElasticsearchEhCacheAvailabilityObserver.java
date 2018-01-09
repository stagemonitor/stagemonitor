package org.stagemonitor.ehcache;

import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchEhCacheAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		elasticsearchClient.sendMetricDashboardBulkAsync("kibana/EhCache.bulk");
	}

}

