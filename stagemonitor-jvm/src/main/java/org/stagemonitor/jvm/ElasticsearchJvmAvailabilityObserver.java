package org.stagemonitor.jvm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchJvmAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchJvmAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient) {
		logger.info("sending kibana jvm bulk...");
		elasticsearchClient.sendClassPathRessourceBulkAsync("kibana/JVM.bulk", true);
		corePlugin.getGrafanaClient().sendGrafanaDashboardAsync("grafana/ElasticsearchJvm.json");
		logger.info("sent kibana jvm bulk...");
	}

	@Override
	public int getPriority() {
		return 0;
	}
}
