package org.stagemonitor.core.elasticsearch;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;

public abstract class AbstractElasticsearchInitializer implements ElasticsearchAvailabilityObserver {
	private boolean hasRun = false;
	protected CorePlugin corePlugin;

	@Override
	public void init(ConfigurationRegistry configurationRegistry) {
		this.corePlugin = configurationRegistry.getConfig(CorePlugin.class);
	}

	public final void onElasticsearchAvailable(ElasticsearchClient elasticsearchClient) {
		if (!hasRun) {
			onElasticsearchFirstAvailable(elasticsearchClient);
			hasRun = true;
		}
	}

	protected abstract void onElasticsearchFirstAvailable(ElasticsearchClient elasticsearchClient);

	@Override
	public int getPriority() {
		return 0;
	}
}
