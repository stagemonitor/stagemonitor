package org.stagemonitor.tracing.elasticsearch;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchInitIndicesAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private ElasticsearchTracingPlugin elasticsearchTracingPlugin;

	@Override
	public void init(ConfigurationRegistry configurationRegistry) {
		super.init(configurationRegistry);
		this.elasticsearchTracingPlugin = configurationRegistry.getConfig(ElasticsearchTracingPlugin.class);
	}

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		createSpansIndex(elasticsearchClient);

		final String resourcePath = elasticsearchClient.getElasticsearchResourcePath();
		elasticsearchClient.updateKibanaIndexPattern("stagemonitor-spans-*",resourcePath + "stagemonitor-spans-kibana-index-pattern.json");

		elasticsearchClient.sendSpanDashboardBulkAsync(resourcePath + "Request-Analysis.bulk", true);
		elasticsearchClient.sendSpanDashboardBulkAsync(resourcePath + "Web-Analytics.bulk", true);

		elasticsearchClient.scheduleIndexManagement("stagemonitor-spans-",
				corePlugin.getMoveToColdNodesAfterDays(), elasticsearchTracingPlugin.getDeleteSpansAfterDays());
	}

	private void createSpansIndex(ElasticsearchClient elasticsearchClient) {
		final String spanMappingJson = ElasticsearchClient.modifyIndexTemplate(
				elasticsearchTracingPlugin.getSpanIndexTemplate(), corePlugin.getMoveToColdNodesAfterDays(), corePlugin.getNumberOfReplicas(), corePlugin.getNumberOfShards());
		elasticsearchClient.sendMappingTemplate(spanMappingJson, "stagemonitor-spans");
		elasticsearchClient.createEmptyIndex(ElasticsearchSpanReporter.getTodaysIndexName());
	}
}
