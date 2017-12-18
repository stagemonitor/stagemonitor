package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.util.IOUtils;

public class ElasticsearchCoreAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchCoreAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable() {
		logger.info("sending Elasticsearch CustomMetricsDashboard...");
		grafanaClient.createElasticsearchDatasource(elasticsearchClient.getElasticsearchUrl());
		grafanaClient.sendGrafanaDashboardAsync("grafana/ElasticsearchCustomMetricsDashboard.json");
		logger.info("sent Elasticsearch CustomMetricsDashboard...");
		logger.info("creating KibanaIndexAndMapping...");
		createKibanaIndexAndMappings();
		logger.info("created KibanaIndexAndMapping...");

	}

	private void createKibanaIndexAndMappings() {
		// makes sure the .kibana index is present and has the right mapping.
		// otherwise it leads to problems if stagemonitor sends the dashboards to the
		// .kibana index before it has been properly created by kibana
		elasticsearchClient.createIndexAndSendMappingAsync(".kibana", "index-pattern", IOUtils.getResourceAsStream("kibana/kibana-index-index-pattern.json"));
		elasticsearchClient.createIndexAndSendMappingAsync(".kibana", "search", IOUtils.getResourceAsStream("kibana/kibana-index-search.json"));
		elasticsearchClient.createIndexAndSendMappingAsync(".kibana", "dashboard", IOUtils.getResourceAsStream("kibana/kibana-index-dashboard.json"));
		elasticsearchClient.createIndexAndSendMappingAsync(".kibana", "visualization", IOUtils.getResourceAsStream("kibana/kibana-index-visualization.json"));
	}

	@Override
	public int getPriority() {
		return Integer.MAX_VALUE;
	}
}
