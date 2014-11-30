package org.stagemonitor.springmvc;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

import java.util.ArrayList;
import java.util.List;

public class SpringMvcPlugin extends StagemonitorPlugin {

	private ConfigurationOption<Boolean> onlyMvcOption = ConfigurationOption.booleanOption()
			.key("stagemonitor.requestmonitor.spring.monitorOnlySpringMvcRequests")
			.dynamic(true)
			.label("Monitor only SpringMVC requests")
			.description("Whether or not requests should be ignored, if they will not be handled by a Spring MVC controller method.\n" +
					"This is handy, if you are not interested in the performance of serving static files. " +
					"Setting this to true can also significantly reduce the amount of files (and thus storing space) " +
					"Graphite will allocate.")
			.defaultValue(false)
			.configurationCategory("Spring MVC Plugin")
			.build();

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		List<ConfigurationOption<?>> config = new ArrayList<ConfigurationOption<?>>();
		config.add(onlyMvcOption);
		return config;
	}

	public boolean isMonitorOnlySpringMvcRequests() {
		return onlyMvcOption.getValue();
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
	}
}
