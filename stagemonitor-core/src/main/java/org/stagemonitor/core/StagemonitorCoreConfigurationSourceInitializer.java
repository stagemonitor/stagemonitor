package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.ElasticsearchConfigurationSource;
import org.stagemonitor.configuration.source.EnvironmentVariableConfigurationSource;
import org.stagemonitor.configuration.source.PropertyFileConfigurationSource;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.configuration.source.SystemPropertyConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

import java.io.IOException;
import java.util.Collection;

public class StagemonitorCoreConfigurationSourceInitializer extends StagemonitorConfigurationSourceInitializer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void modifyConfigurationSources(ModifyArguments modifyArguments) {
		modifyArguments.addConfigurationSourceAsLast(new SimpleSource());
		modifyArguments.addConfigurationSourceAsLast(new SystemPropertyConfigurationSource());
		final String stagemonitorPropertyOverridesLocation = System.getProperty("stagemonitor.property.overrides");
		if (stagemonitorPropertyOverridesLocation != null) {
			logger.info("try loading of default property overrides: '" + stagemonitorPropertyOverridesLocation + "'");
			modifyArguments.addConfigurationSourceAsLast(new PropertyFileConfigurationSource(stagemonitorPropertyOverridesLocation));
		}
		if (PropertyFileConfigurationSource.isPresent("stagemonitor.properties")) {
			modifyArguments.addConfigurationSourceAsLast(new PropertyFileConfigurationSource("stagemonitor.properties"));
		}
		modifyArguments.addConfigurationSourceAsLast(new EnvironmentVariableConfigurationSource());
	}

	@Override
	public void onConfigurationInitialized(ConfigInitializedArguments configInitializedArguments) throws IOException {
		final CorePlugin corePlugin = configInitializedArguments.getConfiguration().getConfig(CorePlugin.class);
		final Collection<String> elasticsearchConfigurationSourceIds = corePlugin.getElasticsearchConfigurationSourceProfiles();
		if (!elasticsearchConfigurationSourceIds.isEmpty()) {
			addElasticsearchConfigurationSources(configInitializedArguments.getConfiguration(), corePlugin, elasticsearchConfigurationSourceIds);
		}
	}

	private void addElasticsearchConfigurationSources(ConfigurationRegistry configuration, CorePlugin corePlugin, Collection<String> elasticsearchConfigurationSourceIds) {
		ElasticsearchClient elasticsearchClient = configuration.getConfig(CorePlugin.class).getElasticsearchClient();
		if (corePlugin.isDeactivateStagemonitorIfEsConfigSourceIsDown()) {
			assertElasticsearchIsAvailable(elasticsearchClient, corePlugin);
		}

		for (String configurationId : elasticsearchConfigurationSourceIds) {
			final ElasticsearchConfigurationSource esSource = new ElasticsearchConfigurationSource(elasticsearchClient, configurationId);
			configuration.addConfigurationSourceAfter(esSource, SimpleSource.class);
		}
		configuration.reloadAllConfigurationOptions();
	}

	private void assertElasticsearchIsAvailable(ElasticsearchClient elasticsearchClient, CorePlugin corePlugin) {
		try {
			elasticsearchClient.getJson("/");
		} catch (IOException e) {
			throw new IllegalStateException("Property stagemonitor.elasticsearch.configurationSourceProfiles was set " +
					"but elasticsearch is not reachable at " + corePlugin.getElasticsearchUrl(), e);
		}
	}
}
