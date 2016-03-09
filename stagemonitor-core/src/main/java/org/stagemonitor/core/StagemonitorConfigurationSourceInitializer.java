package org.stagemonitor.core;

import java.util.List;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

/**
 * This SPI provides callback methods that can be used to modify stagemonitor's configuration and configuration sources.
 * <p/>
 * In order to register a callback, you have to implement this interface and create the file
 * <code>/META-INF/services/org.stagemonitor.core.StagemonitorConfigurationSourceInitializer</code> with the canonical
 * class name of the implementation as its content.
 */
public abstract class StagemonitorConfigurationSourceInitializer implements StagemonitorSPI {

	/**
	 * This method is called before just before the initialisation of the stagemonitor {@link Configuration}.
	 * <p/>
	 * This callback can be used to initialize or modify the list of configuration sources.
	 *
	 * @param modifyArguments
	 */
	public abstract void modifyConfigurationSources(ModifyArguments modifyArguments);

	/**
	 * This method is called as soon as the stagemonitor {@link Configuration} has been initialized and before anything
	 * has been read from it.
	 *
	 *
	 * @param configInitializedArguments@throws Exception if there was a initialisation error. If a exception is thrown, stagemonitor will be deactivated!
	 */
	public void onConfigurationInitialized(ConfigInitializedArguments configInitializedArguments) throws Exception {
	}

	public static class ModifyArguments {
		private final List<ConfigurationSource> configurationSources;

		/**
		 * @param configurationSources the mutable list of currently registered configuration sources
		 */
		ModifyArguments(List<ConfigurationSource> configurationSources) {
			this.configurationSources = configurationSources;
		}

		public void addConfigurationSourceAsFirst(ConfigurationSource configurationSource) {
			configurationSources.add(0, configurationSource);
		}

		public void addConfigurationSourceAsLast(ConfigurationSource configurationSource) {
			configurationSources.add(configurationSource);
		}
	}

	public static class ConfigInitializedArguments {
		private final Configuration configuration;

		/**
		 * @param configuration the configuration that has just been initialized
		 */
		ConfigInitializedArguments(Configuration configuration) {
			this.configuration = configuration;
		}

		public Configuration getConfiguration() {
			return configuration;
		}
	}
}
