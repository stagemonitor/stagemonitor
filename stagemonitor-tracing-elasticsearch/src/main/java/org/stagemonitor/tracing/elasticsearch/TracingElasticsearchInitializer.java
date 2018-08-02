package org.stagemonitor.tracing.elasticsearch;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchInitializer;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class TracingElasticsearchInitializer extends AbstractElasticsearchInitializer {

	private ElasticsearchTracingPlugin elasticsearchTracingPlugin;

	@Override
	public void init(ConfigurationRegistry configurationRegistry) {
		super.init(configurationRegistry);
		this.elasticsearchTracingPlugin = configurationRegistry.getConfig(ElasticsearchTracingPlugin.class);
	}

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		if (corePlugin.isInitializeElasticsearch()) {
			createSpansIndex(elasticsearchClient);

			final String resourcePath = elasticsearchClient.getKibanaResourcePath();
			elasticsearchClient.updateKibanaIndexPattern("stagemonitor-spans-*",resourcePath + "stagemonitor-spans-kibana-index-pattern.json");

			elasticsearchClient.sendSpanDashboardBulkAsync(resourcePath + "Request-Analysis.bulk", true);
			elasticsearchClient.sendSpanDashboardBulkAsync(resourcePath + "Web-Analytics.bulk", true);
		}

		elasticsearchClient.scheduleIndexManagement("stagemonitor-spans-",
				corePlugin.getMoveToColdNodesAfterDays(), elasticsearchTracingPlugin.getDeleteSpansAfterDays());
	}

	private void createSpansIndex(ElasticsearchClient elasticsearchClient) {
		String templatePath = elasticsearchTracingPlugin.getSpanIndexTemplate();
		if (elasticsearchTracingPlugin.isSpanIndexTemplateDefaultValue()) {
			templatePath = elasticsearchClient.getElasticSearchTemplateResourcePath() + elasticsearchTracingPlugin.getSpanIndexTemplate();
		}
		final String spanMappingJson = ElasticsearchClient.modifyIndexTemplate(
				templatePath, corePlugin.getMoveToColdNodesAfterDays(), corePlugin.getNumberOfReplicas(), corePlugin.getNumberOfShards());
		String templateName = "stagemonitor-spans";
		if (elasticsearchClient.isElasticsearch7Compatible()) {
			templateName = "stagemonitor-spans-*";
		}
		elasticsearchClient.sendMappingTemplate(spanMappingJson, templateName);
		elasticsearchClient.createEmptyIndex(ElasticsearchSpanReporter.getTodaysIndexName());
	}
}
