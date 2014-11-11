package org.stagemonitor.core;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;

import java.util.Collection;

public interface StagemonitorConfigurationSourceInitializer {

	void modifyConfigurationSources(Collection<ConfigurationSource> configurationSources);

	/**
	 *
	 * @param configuration
	 * @throws Exception if there was a initialisation error. If a exception is thrown, stagemonitor will be deactivated!
	 */
	void onConfigurationInitialized(Configuration configuration) throws Exception;
}
