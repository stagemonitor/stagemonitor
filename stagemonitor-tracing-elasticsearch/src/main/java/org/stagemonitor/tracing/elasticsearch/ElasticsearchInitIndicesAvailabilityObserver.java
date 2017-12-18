package org.stagemonitor.tracing.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchInitIndicesAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchInitIndicesAvailabilityObserver.class);
	private ElasticsearchTracingPlugin elasticsearchTracingPlugin;

	@Override
	public void init(ConfigurationRegistry configurationRegistry) {
		super.init(configurationRegistry);
		this.elasticsearchTracingPlugin = configurationRegistry.getConfig(ElasticsearchTracingPlugin.class);
	}

	@Override
	protected void onElasticsearchFirstAvailable() {
		logger.info("sending stagemonitor-spans-* index pattern...");
		final ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
		elasticsearchClient.updateKibanaIndexPatternAsyncForce("kibana/stagemonitor-spans-kibana-index-pattern.json",
						"/.kibana/index-pattern/stagemonitor-spans-*");
		elasticsearchClient.sendClassPathRessourceBulkAsyncForce("kibana/Request-Analysis.bulk", true);
		elasticsearchClient.sendClassPathRessourceBulkAsyncForce("kibana/Web-Analytics.bulk", true);
		final String spanMappingJson = ElasticsearchClient.modifyIndexTemplate(
						elasticsearchTracingPlugin.getSpanIndexTemplate(), corePlugin.getMoveToColdNodesAfterDays(), corePlugin.getNumberOfReplicas(), corePlugin.getNumberOfShards());
		elasticsearchClient.sendMappingTemplateAsync(spanMappingJson, "stagemonitor-spans");
		elasticsearchClient.createEmptyIndexAsync(ElasticsearchSpanReporter.getTodaysIndexName());
		logger.info("sent stagemonitor-spans-* index pattern!");
	}
}
