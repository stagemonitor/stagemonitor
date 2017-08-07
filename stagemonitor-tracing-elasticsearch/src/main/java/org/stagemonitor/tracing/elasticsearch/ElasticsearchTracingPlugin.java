package org.stagemonitor.tracing.elasticsearch;

import org.stagemonitor.configuration.ConfigurationOption;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.tracing.TracingPlugin;

import java.util.Collections;
import java.util.List;

import static org.stagemonitor.tracing.elasticsearch.ElasticsearchSpanReporter.bulkSizeMetricName;
import static org.stagemonitor.tracing.elasticsearch.ElasticsearchSpanReporter.spansDroppedMetricName;

public class ElasticsearchTracingPlugin extends StagemonitorPlugin {

	public static final String ELASTICSEARCH_TRACING_PLUGIN = "Elasticsearch trace storage plugin";

	private final ConfigurationOption<Boolean> onlyLogElasticsearchSpanReports = ConfigurationOption.booleanOption()
			.key("stagemonitor.tracing.elasticsearch.onlyLogElasticsearchRequestTraceReports")
			.aliasKeys("stagemonitor.requestmonitor.elasticsearch.onlyLogElasticsearchRequestTraceReports")
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
			.key("stagemonitor.tracing.elasticsearch.spanIndexTemplate")
			.aliasKeys("stagemonitor.requestmonitor.elasticsearch.spanIndexTemplate")
			.dynamic(false)
			.label("ES Request Span Template")
			.description("The classpath location of the index template that is used for the stagemonitor-spans-* indices. " +
					"By specifying the location to your own template, you can fully customize the index template.")
			.configurationCategory(ELASTICSEARCH_TRACING_PLUGIN)
			.tags("elasticsearch")
			.buildWithDefault("stagemonitor-elasticsearch-span-index-template.json");
	private final ConfigurationOption<Integer> deleteSpansAfterDays = ConfigurationOption.integerOption()
			.key("stagemonitor.tracing.elasticsearch.deleteSpansAfterDays")
			.aliasKeys("stagemonitor.requestmonitor.deleteRequestTracesAfterDays")
			.dynamic(true)
			.label("Delete spans after (days)")
			.description("When set, spans will be deleted automatically after the specified days. " +
					"Set to a negative value to never delete spans.")
			.configurationCategory(ELASTICSEARCH_TRACING_PLUGIN)
			.buildWithDefault(-1);
	private final ConfigurationOption<Integer> maxBatchSize = ConfigurationOption.integerOption()
			.key("stagemonitor.tracing.elasticsearch.reporter.maxBatchSize")
			.dynamic(true)
			.label("Max batch size")
			.description("The maximum number of spans which are batched into a single _bulk request to elasticsearch. " +
					"If the span queue size exceeds the max batch size, a flush will be scheduled immediately. " +
					"You can monitor the amount of reported spans and the actual batch sizes via the histogram " + bulkSizeMetricName.getName())
			.configurationCategory(ELASTICSEARCH_TRACING_PLUGIN)
			.buildWithDefault(250);
	private final ConfigurationOption<Integer> flushDelayMs = ConfigurationOption.integerOption()
			.key("stagemonitor.tracing.elasticsearch.reporter.flushDelayMs")
			.dynamic(true)
			.label("Flush delay (ms)")
			.description("The maximum amount of time between two flushes.")
			.configurationCategory(ELASTICSEARCH_TRACING_PLUGIN)
			.buildWithDefault(1000);
	private final ConfigurationOption<Integer> maxQueueSize = ConfigurationOption.integerOption()
			.key("stagemonitor.tracing.elasticsearch.reporter.maxSpanQueueSize")
			.dynamic(false)
			.label("Max span queue size")
			.description("The maximum amount of elements in the span queue. " +
					"If the queue is full and new spans arrive, they are dropped. " +
					"You can monitor the amount of dropped spans via the counter " + spansDroppedMetricName.getName())
			.configurationCategory(ELASTICSEARCH_TRACING_PLUGIN)
			.buildWithDefault(1000);

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

			elasticsearchClient.scheduleIndexManagement("stagemonitor-spans-",
					corePlugin.getMoveToColdNodesAfterDays(), deleteSpansAfterDays.getValue());
		}
	}

	@Override
	public List<Class<? extends StagemonitorPlugin>> dependsOn() {
		return Collections.<Class<? extends StagemonitorPlugin>>singletonList(TracingPlugin.class);
	}

	public boolean isOnlyLogElasticsearchSpanReports() {
		return onlyLogElasticsearchSpanReports.getValue();
	}

	public int getMaxBatchSize() {
		return maxBatchSize.getValue();
	}

	public int getFlushDelayMs() {
		return flushDelayMs.getValue();
	}

	public int getMaxQueueSize() {
		return maxQueueSize.getValue();
	}
}
