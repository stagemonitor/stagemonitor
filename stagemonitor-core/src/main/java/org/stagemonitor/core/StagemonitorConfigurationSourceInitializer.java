package org.stagemonitor.core;

import java.util.List;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

/**
 * This interface provides callback methods that can be used to modify stagemonitor's configuration and configuration sources.
 * <p/>
 * In order to register a callback, you have to implement this interface and create the file
 * <code>/META-INF/services/org.stagemonitor.core.StagemonitorConfigurationSourceInitializer</code> with the canonical
 * class name of the implementation as its content.
 *
 */
public interface StagemonitorConfigurationSourceInitializer {

	/**
	 * This method is called before just before the initialisation of the stagemonitor {@link Configuration}.
	 * <p/>
	 * This callback can be used to initialize or modify the list of configuration sources.
	 *
	 * @param configurationSources the mutable list of currently registered configuration sources
	 */
	void modifyConfigurationSources(List<ConfigurationSource> configurationSources);

	/**
	 * This method is called as soon as the stagemonitor {@link Configuration} has been initialized and before anything
	 * has been read from it.
	 *
	 * @param configuration the configuration that has just been initialized
	 * @throws Exception if there was a initialisation error. If a exception is thrown, stagemonitor will be deactivated!
	 */
	void onConfigurationInitialized(Configuration configuration) throws Exception;
}
