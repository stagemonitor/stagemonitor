package org.stagemonitor.jvm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.AbstractElasticsearchFirstAvailabilityObserver;

public class ElasticsearchJvmAvailabilityObserver extends AbstractElasticsearchFirstAvailabilityObserver {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchJvmAvailabilityObserver.class);

	@Override
	protected void onElasticsearchFirstAvailable() {
		logger.info("sending kibana jvm bulk...");
		corePlugin.getElasticsearchClient().sendClassPathRessourceBulkAsync("kibana/JVM.bulk", true);
		corePlugin.getGrafanaClient().sendGrafanaDashboardAsync("grafana/ElasticsearchJvm.json");
		logger.info("sent kibana jvm bulk...");
	}
}
