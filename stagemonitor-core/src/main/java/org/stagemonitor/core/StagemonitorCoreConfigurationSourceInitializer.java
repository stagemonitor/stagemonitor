package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.EnvironmentVariableConfigurationSource;
import org.stagemonitor.configuration.source.PropertyFileConfigurationSource;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.configuration.source.SystemPropertyConfigurationSource;
import org.stagemonitor.core.configuration.ElasticsearchConfigurationSource;
import org.stagemonitor.core.configuration.RemotePropertiesConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.http.HttpRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

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

		if (!corePlugin.getRemotePropertiesConfigUrls().isEmpty()) {
			logger.debug("RemotePropertiesConfigurationSource is enabled");
			addRemotePropertiesConfigurationSources(configInitializedArguments.getConfiguration(), corePlugin);
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
			throw new IllegalStateException("Property stagemonitor.configuration.elasticsearch.configurationSourceProfiles was set " +
					"but elasticsearch is not reachable at " + corePlugin.getElasticsearchUrl(), e);
		}
	}

	/**
	 * Creates and registers a RemotePropertiesConfigurationSource for each configuration url
	 */
	private void addRemotePropertiesConfigurationSources(ConfigurationRegistry configuration, CorePlugin corePlugin) {
		// Validating necessary properties
		final List<String> configurationUrls = new ArrayList<String>(corePlugin.getRemotePropertiesConfigUrls());

		if (corePlugin.isDeactivateStagemonitorIfRemotePropertyServerIsDown()) {
			assertRemotePropertiesServerIsAvailable(configurationUrls.get(0));
		}

		logger.debug("Loading RemotePropertiesConfigurationSources with: configurationUrls = " + configurationUrls);
		final HttpClient sharedHttpClient = new HttpClient();
		for (String configUrl : configurationUrls) {
			final RemotePropertiesConfigurationSource source = new RemotePropertiesConfigurationSource(
					sharedHttpClient,
					configUrl);
			configuration.addConfigurationSourceAfter(source, SimpleSource.class);
		}

		configuration.reloadAllConfigurationOptions();
	}

	/**
	 * Does a simple HEAD request to a configuration endpoint to check if it's reachable. If not an
	 * IllegalStateException is thrown
	 *
	 * @param configUrl Full qualified configuration url
	 */
	private void assertRemotePropertiesServerIsAvailable(final String configUrl) {
		new HttpClient().send(
				"HEAD",
				configUrl,
				new HashMap<String, String>(),
				null,
				new HttpClient.ResponseHandler<Void>() {
					@Override
					public Void handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
						if (e != null || statusCode != 200) {
							throw new IllegalStateException("Remote properties are not available at " +
									configUrl + ", http status code: " + statusCode, e);
						}
						return null;
					}
				}
		);
	}
}
