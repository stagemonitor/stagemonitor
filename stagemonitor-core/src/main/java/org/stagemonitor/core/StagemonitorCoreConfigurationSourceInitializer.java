package org.stagemonitor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.EnvironmentVariableConfigurationSource;
import org.stagemonitor.configuration.source.PropertyFileConfigurationSource;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.configuration.source.SystemPropertyConfigurationSource;
import org.stagemonitor.core.configuration.ElasticsearchConfigurationSource;
import org.stagemonitor.core.configuration.SpringCloudConfigConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.http.HttpRequest;
import org.stagemonitor.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;

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

		if (corePlugin.isSpringCloudConfigurationSourceEnabled()) {
			logger.debug("Spring Cloud Config configuration source is enabled");
			addSpringCloudConfigurationSources(configInitializedArguments.getConfiguration(), corePlugin);
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
	 * Creates and registers a SpringCloudConfigConfigurationSource for each active profile, if the
	 * SpringCloudConfigurationSource is enabled per flag
	 */
	private void addSpringCloudConfigurationSources(ConfigurationRegistry configuration, CorePlugin corePlugin) {
		// Validating necessary properties
		final String applicationName = corePlugin.getApplicationName();
		final String springCloudConfigServerAddress = corePlugin.getSpringCloudConfigServerAddress();
		if (StringUtils.isEmpty(applicationName) || CorePlugin.DEFAULT_APPLICATION_NAME.equals(applicationName)) {
			logger.warn("stagemonitor.applicationName is not set (explicitly) but necessary for the config service configuration source." +
					"Will skip the config source");
			return;
		}
		if (StringUtils.isEmpty(springCloudConfigServerAddress) || !springCloudConfigServerAddress.startsWith("http")) {
			logger.warn("stagemonitor.configuration.springcloud.address is not set or not a valid http/s address but necessary for the Spring Cloud Config configuration source." +
					"Will skip the config source.");
			return;
		}

		final Collection<String> springCloudConfigurationSourceIds = corePlugin.getSpringCloudConfigurationSourceProfiles();
		if (springCloudConfigurationSourceIds.isEmpty()) {
			logger.warn("No configServerConfigurationProfiles set. Using " + SpringCloudConfigConfigurationSource.DEFAULT_PROFILE);
			springCloudConfigurationSourceIds.add(SpringCloudConfigConfigurationSource.DEFAULT_PROFILE);
		}

		if (corePlugin.isDeactivateStagemonitorIfConfigServerIsDown()) {
			assertCloudConfigServerIsAvailable(springCloudConfigServerAddress);
		}

		logger.debug("Loading SpringCloudConfigurationSources with: applicationName = " + applicationName
				+ ", springCloudConfigServerAddress = " + springCloudConfigServerAddress
				+ ", profiles = " + springCloudConfigurationSourceIds);

		final HttpClient sharedHttpClient = new HttpClient();
		for (String configurationId : springCloudConfigurationSourceIds) {
			final SpringCloudConfigConfigurationSource source = new SpringCloudConfigConfigurationSource(
					sharedHttpClient,
					springCloudConfigServerAddress,
					applicationName,
					configurationId);
			configuration.addConfigurationSourceAfter(source, SimpleSource.class);
		}
		configuration.reloadAllConfigurationOptions();
	}


	/**
	 * Does a simple HEAD request to the server's /health endpoint to check if it's reachable If not an
	 * IllegalStateException is thrown
	 *
	 * @param springCloudConfigServerAddress Address of the Spring Cloud Config server
	 */
	private void assertCloudConfigServerIsAvailable(final String springCloudConfigServerAddress) {
		new HttpClient().send("HEAD", springCloudConfigServerAddress + "/health", new HashMap<String, String>(), null, new HttpClient.ResponseHandler<Void>() {
					@Override
					public Void handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
						if (e != null || statusCode != 200) {
							throw new IllegalStateException("Property stagemonitor.configuration.springcloud.enabled was set " +
									"but the config server is not reachable at " + springCloudConfigServerAddress + ", http status code: " + statusCode, e);
						}
						return null;
					}
				}
		);
	}
}
