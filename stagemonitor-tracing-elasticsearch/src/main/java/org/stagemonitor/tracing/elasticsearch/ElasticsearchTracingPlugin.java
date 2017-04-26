package org.stagemonitor.tracing.elasticsearch;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchTracingPlugin extends StagemonitorPlugin {

	public static final String ELASTICSEARCH_TRACING_PLUGIN = "Elasticsearch trace storage plugin";

	private final ConfigurationOption<Boolean> onlyLogElasticsearchSpanReports = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.elasticsearch.onlyLogElasticsearchRequestTraceReports")
			.dynamic(true)
			.label("Only log Elasticsearch request trace reports")
			.description(String.format("If set to true, the spans won't be reported to elasticsearch but instead logged in bulk format. " +
					"The name of the logger is %s. That way you can redirect the reporting to a separate log file and use logstash or a " +
					"different external process to send the spans to elasticsearch.", ElasticsearchSpanReporter.ES_SPAN_LOGGER))
			.tags("reporting")
			.configurationCategory(ELASTICSEARCH_TRACING_PLUGIN)
			.buildWithDefault(false);
	/* Storage */
	private final ConfigurationOption<String> spanIndexTemplate = ConfigurationOption.stringOption()
			.key("stagemonitor.requestmonitor.elasticsearch.spanIndexTemplate")
			.dynamic(false)
			.label("ES Request Span Template")
			.description("The classpath location of the index template that is used for the stagemonitor-spans-* indices. " +
					"By specifying the location to your own template, you can fully customize the index template.")
			.configurationCategory(ELASTICSEARCH_TRACING_PLUGIN)
			.tags("elasticsearch")
			.buildWithDefault("stagemonitor-elasticsearch-span-index-template.json");
	private final ConfigurationOption<Integer> deleteSpansAfterDays = ConfigurationOption.integerOption()
			.key("stagemonitor.requestmonitor.deleteRequestTracesAfterDays")
			.dynamic(true)
			.label("Delete spans after (days)")
			.description("When set, spans will be deleted automatically after the specified days. " +
					"Set to a negative value to never delete spans.")
			.configurationCategory(ELASTICSEARCH_TRACING_PLUGIN)
			.buildWithDefault(7);

	@Override
	public void initializePlugin(InitArguments initArguments) throws Exception {
		final CorePlugin corePlugin = initArguments.getPlugin(CorePlugin.class);
		final ElasticsearchClient elasticsearchClient = corePlugin.getElasticsearchClient();

		final String spanMappingJson = ElasticsearchClient.modifyIndexTemplate(
				spanIndexTemplate.getValue(), corePlugin.getMoveToColdNodesAfterDays(), corePlugin.getNumberOfReplicas(), corePlugin.getNumberOfShards());
		elasticsearchClient.sendMappingTemplateAsync(spanMappingJson, "stagemonitor-spans");

		if (!corePlugin.getElasticsearchUrls().isEmpty()) {
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/stagemonitor-spans-kibana-index-pattern.bulk");
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Request-Analysis.bulk");
			elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/Web-Analytics.bulk");

			elasticsearchClient.scheduleIndexManagement("stagemonitor-external-requests-",
					corePlugin.getMoveToColdNodesAfterDays(), deleteSpansAfterDays.getValue());
		}
	}

	public boolean isOnlyLogElasticsearchSpanReports() {
		return onlyLogElasticsearchSpanReports.getValue();
	}

}
