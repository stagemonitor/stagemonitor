package org.stagemonitor.core.configuration;

import java.util.List;

public interface ConfigurationOptionProvider {

	List<ConfigurationOption<?>> getConfigurationOptions();

}
