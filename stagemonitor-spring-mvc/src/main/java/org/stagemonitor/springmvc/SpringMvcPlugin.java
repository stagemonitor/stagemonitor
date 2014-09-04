package org.stagemonitor.springmvc;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.ConfigurationOption;
import org.stagemonitor.core.StageMonitorPlugin;

import java.util.ArrayList;
import java.util.List;

public class SpringMvcPlugin implements StageMonitorPlugin {

	public static final String MONITOR_ONLY_SPRING_MVC_REQUESTS = "stagemonitor.requestmonitor.spring.monitorOnlySpringMvcRequests";

	@Override
	public List<ConfigurationOption> getConfigurationOptions() {
		List<ConfigurationOption> config = new ArrayList<ConfigurationOption>();
		config.add(ConfigurationOption.builder()
				.key(MONITOR_ONLY_SPRING_MVC_REQUESTS)
				.dynamic(true)
				.label("Monitor only SpringMVC requests")
				.description("Whether or not requests should be ignored, if they will not be handled by a Spring MVC controller method.\n" +
						"This is handy, if you are not interested in the performance of serving static files. " +
						"Setting this to <code>true</code> can also significantly reduce the amount of files (and thus storing space) " +
						"Graphite will allocate.")
				.defaultValue("false")
				.build());
		return config;
	}

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) {
	}
}
