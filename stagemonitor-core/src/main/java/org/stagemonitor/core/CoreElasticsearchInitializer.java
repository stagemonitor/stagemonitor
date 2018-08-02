package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchInitializer;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.metrics.metrics2.ElasticsearchReporter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.util.IOUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CoreElasticsearchInitializer extends AbstractElasticsearchInitializer {

	private static final Logger logger = LoggerFactory.getLogger(CoreElasticsearchInitializer.class);

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		if (corePlugin.isInitializeElasticsearch()) {
			if (elasticsearchClient.isElasticsearch6Compatible() || elasticsearchClient.isElasticsearch7Compatible()) {
				logger.debug("creating KibanaIndexAndMapping for ES 6/7...");
				elasticsearchClient.createIndexAndSendMapping(".kibana", "doc", IOUtils.getResourceAsStream(elasticsearchClient.getKibanaResourcePath() + "kibana-index-doc.json"));
				logger.debug("created KibanaIndexAndMapping for ES 6/7");
			} else {
				logger.debug("creating KibanaIndexAndMapping for ES 5...");
				createKibana5IndexAndMappings(elasticsearchClient);
				sendConfigurationMapping(elasticsearchClient);
				logger.debug("created KibanaIndexAndMapping for ES 5");
			}
			manageMetricsIndex(elasticsearchClient, corePlugin);
		}

		elasticsearchClient.scheduleIndexManagement(ElasticsearchReporter.STAGEMONITOR_METRICS_INDEX_PREFIX,
				corePlugin.getMoveToColdNodesAfterDays(), corePlugin.getDeleteElasticsearchMetricsAfterDays());

		reportToElasticsearch(corePlugin.getMetricRegistry(), corePlugin.getElasticsearchReportingInterval(), corePlugin.getMeasurementSession());
	}

	private void createKibana5IndexAndMappings(ElasticsearchClient elasticsearchClient) {
		// makes sure the .kibana index is present and has the right mapping.
		// otherwise it leads to problems if stagemonitor sends the dashboards to the
		// .kibana index before it has been properly created by kibana
		final String resourcePath = elasticsearchClient.getKibanaResourcePath();
		elasticsearchClient.createIndexAndSendMapping(".kibana", "index-pattern", IOUtils.getResourceAsStream(resourcePath + "kibana-index-index-pattern.json"));
		elasticsearchClient.createIndexAndSendMapping(".kibana", "search", IOUtils.getResourceAsStream(resourcePath + "kibana-index-search.json"));
		elasticsearchClient.createIndexAndSendMapping(".kibana", "dashboard", IOUtils.getResourceAsStream(resourcePath + "kibana-index-dashboard.json"));
		elasticsearchClient.createIndexAndSendMapping(".kibana", "visualization", IOUtils.getResourceAsStream(resourcePath + "kibana-index-visualization.json"));
	}

	public static void sendConfigurationMapping(ElasticsearchClient elasticsearchClient) {
		final String mappingJson;
		try {
			mappingJson = IOUtils.toString(IOUtils.getResourceAsStream("stagemonitor-configuration-elasticsearch-mapping.json"));
			elasticsearchClient.sendMappingTemplate(mappingJson, "stagemonitor-configuration");
		} catch (IOException e) {
			logger.warn("Suppressed exception:", e);
		}
	}

	private static void manageMetricsIndex(ElasticsearchClient elasticsearchClient, CorePlugin corePlugin) {
		if (corePlugin.isReportToElasticsearch()) {
			elasticsearchClient.updateKibanaIndexPattern("stagemonitor-metrics-*", elasticsearchClient.getKibanaResourcePath() + "stagemonitor-metrics-kibana-index-pattern.json");
			String templatePath = corePlugin.getMetricsIndexTemplate();
			if (corePlugin.isMetricsIndexTemplateDefaultValue()) {
				templatePath = elasticsearchClient.getElasticSearchTemplateResourcePath() + corePlugin.getMetricsIndexTemplate();
			}
			final String mappingJson = ElasticsearchClient.modifyIndexTemplate(
					templatePath, corePlugin.getMoveToColdNodesAfterDays(), corePlugin.getNumberOfReplicas(), corePlugin.getNumberOfShards());
			String templateName = "stagemonitor-metrics";
			if (elasticsearchClient.isElasticsearch7Compatible()) {
				templateName = "stagemonitor-metrics-*";
			}
			elasticsearchClient.sendMappingTemplate(mappingJson, templateName);
			elasticsearchClient.createEmptyIndex(ElasticsearchReporter.getTodaysIndexName());
		}
	}

	private void reportToElasticsearch(Metric2Registry metricRegistry, int reportingInterval,
									   final MeasurementSession measurementSession) {
		if (corePlugin.isReportToElasticsearch()) {
			logger.info("Sending metrics to Elasticsearch ({}) every {}s", corePlugin.getElasticsearchUrlsWithoutAuthenticationInformation(), reportingInterval);
		}
		if (corePlugin.isReportToElasticsearch() || corePlugin.isOnlyLogElasticsearchMetricReports()) {
			final ElasticsearchReporter reporter = ElasticsearchReporter.forRegistry(metricRegistry, corePlugin)
					.globalTags(measurementSession.asMap())
					.build();

			reporter.start(reportingInterval, TimeUnit.SECONDS);
			corePlugin.closeOnShutdown(reporter);
		} else {
			logger.info("Not sending metrics to Elasticsearch (url={}, interval={}s)", corePlugin.getElasticsearchUrlsWithoutAuthenticationInformation(), reportingInterval);
		}
	}

	@Override
	public int getPriority() {
		return Integer.MAX_VALUE;
	}
}
