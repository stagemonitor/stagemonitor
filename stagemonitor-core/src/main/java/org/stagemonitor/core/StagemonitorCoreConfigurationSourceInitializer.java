package org.stagemonitor.core;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.ElasticsearchConfigurationSource;
import org.stagemonitor.core.configuration.source.EnvironmentVariableConfigurationSource;
import org.stagemonitor.core.configuration.source.PropertyFileConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;
import org.stagemonitor.core.configuration.source.SystemPropertyConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class StagemonitorCoreConfigurationSourceInitializer implements StagemonitorConfigurationSourceInitializer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void modifyConfigurationSources(List<ConfigurationSource> configurationSources) {
		configurationSources.add(new SimpleSource());
		configurationSources.add(new SystemPropertyConfigurationSource());
		final String stagemonitorPropertyOverridesLocation = System.getProperty("stagemonitor.property.overrides");
		if (stagemonitorPropertyOverridesLocation != null) {
			logger.info("try loading of default property overrides: '" + stagemonitorPropertyOverridesLocation + "'");
			configurationSources.add(new PropertyFileConfigurationSource(stagemonitorPropertyOverridesLocation));
		}
		if (PropertyFileConfigurationSource.isPresent("stagemonitor.properties")) {
			configurationSources.add(new PropertyFileConfigurationSource("stagemonitor.properties"));
		}
		configurationSources.add(new EnvironmentVariableConfigurationSource());
	}

	@Override
	public void onConfigurationInitialized(Configuration configuration) throws IOException {
		final CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		final Collection<String> elasticsearchConfigurationSourceIds = corePlugin.getElasticsearchConfigurationSourceProfiles();
		if (!elasticsearchConfigurationSourceIds.isEmpty()) {
			addElasticsearchConfigurationSources(configuration, corePlugin, elasticsearchConfigurationSourceIds);
		}
	}

	private void addElasticsearchConfigurationSources(Configuration configuration, CorePlugin corePlugin, Collection<String> elasticsearchConfigurationSourceIds) {
		if (corePlugin.isDeactivateStagemonitorIfEsConfigSourceIsDown()) {
			assertElasticsearchIsAvailable(corePlugin);
		}

		for (String configurationId : elasticsearchConfigurationSourceIds) {
			configuration.addConfigurationSource(new ElasticsearchConfigurationSource(configurationId), false);
		}
		configuration.reloadAllConfigurationOptions();
	}

	private void assertElasticsearchIsAvailable(CorePlugin corePlugin) {
		try {
			ElasticsearchClient.getJson("/");
		} catch (IOException e) {
			throw new IllegalStateException("Property stagemonitor.elasticsearch.configurationSourceProfiles was set " +
					"but elasticsearch is not reachable at " + corePlugin.getElasticsearchUrl(), e);
		}
	}
}
