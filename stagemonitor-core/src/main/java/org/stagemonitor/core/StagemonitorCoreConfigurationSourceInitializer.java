package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.ElasticsearchConfigurationSource;
import org.stagemonitor.core.configuration.source.EnvironmentVariableConfigurationSource;
import org.stagemonitor.core.configuration.source.PropertyFileConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;
import org.stagemonitor.core.configuration.source.SystemPropertyConfigurationSource;

import java.io.IOException;
import java.util.Collection;

public class StagemonitorCoreConfigurationSourceInitializer implements StagemonitorConfigurationSourceInitializer {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void modifyConfigurationSources(Collection<ConfigurationSource> configurationSources) {
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
	public void onConfigurationInitialized(Configuration configuration) throws IOException{
		final CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		if (!corePlugin.getElasticsearchConfigurationSourceIds().isEmpty()) {
			try {
				for (String configurationId : corePlugin.getElasticsearchConfigurationSourceIds()) {
					configuration.addConfigurationSource(new ElasticsearchConfigurationSource(configurationId));
				}
			} catch (IOException e) {
				throw new IllegalStateException("Property stagemonitor.elasticsearch.configurationSourceIds was set " +
						"but elasticsearch is not reachable at " + corePlugin.getElasticsearchUrl(), e);
			}
		}
	}
}
