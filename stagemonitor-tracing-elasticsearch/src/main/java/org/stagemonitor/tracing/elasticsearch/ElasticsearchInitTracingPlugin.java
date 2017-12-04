package org.stagemonitor.tracing.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchAvailableObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchInitTracingPlugin implements ElasticsearchAvailableObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchInitTracingPlugin.class);
	private boolean hasRun = false;
	private CorePlugin corePlugin;
	private ElasticsearchTracingPlugin elasticsearchTracingPlugin;

	@Override
	public void init(ConfigurationRegistry configurationRegistry) {
		this.corePlugin = configurationRegistry.getConfig(CorePlugin.class);
		this.elasticsearchTracingPlugin = configurationRegistry.getConfig(ElasticsearchTracingPlugin.class);
	}

	@Override
	public void onElasticsearchAvailable() {
		if (!hasRun) {
			logger.info("sending stagemonitor-spans-* index pattern...");
			final ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();
			elasticsearchClient.updateKibanaIndexPatternAsync("kibana/stagemonitor-spans-kibana-index-pattern.json",
							"/.kibana/index-pattern/stagemonitor-spans-*");
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Request-Analysis.bulk", true);
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Web-Analytics.bulk", true);
			final String spanMappingJson = ElasticsearchClient.modifyIndexTemplate(
							elasticsearchTracingPlugin.getSpanIndexTemplate(), corePlugin.getMoveToColdNodesAfterDays(), corePlugin.getNumberOfReplicas(), corePlugin.getNumberOfShards());
			elasticsearchClient.sendMappingTemplateAsync(spanMappingJson, "stagemonitor-spans");
			elasticsearchClient.createEmptyIndexAsync(ElasticsearchSpanReporter.getTodaysIndexName());
			logger.info("sent stagemonitor-spans-* index pattern!");
			hasRun = true;
		}
	}


}
