package org.stagemonitor.core.elasticsearch;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.grafana.GrafanaClient;

public abstract class AbstractElasticsearchFirstAvailabilityObserver implements ElasticsearchAvailabilityObserver {
	private boolean hasRun = false;
	protected CorePlugin corePlugin;
	protected ElasticsearchClient elasticsearchClient;
	protected GrafanaClient grafanaClient;

	@Override
	public void init(ConfigurationRegistry configurationRegistry) {
		this.corePlugin = configurationRegistry.getConfig(CorePlugin.class);
		this.elasticsearchClient = corePlugin.getElasticsearchClient();
		this.grafanaClient = corePlugin.getGrafanaClient();
	}

	public final void onElasticsearchAvailable() {
		if (!hasRun) {
			onElasticsearchFirstAvailable();
			hasRun = true;
		}
	}

	protected abstract void onElasticsearchFirstAvailable();

	@Override
	public int getPriority() {
		return 0;
	}
}
