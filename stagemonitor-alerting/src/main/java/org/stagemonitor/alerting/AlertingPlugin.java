package org.stagemonitor.alerting;

import java.util.Arrays;
import java.util.List;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

public class AlertingPlugin extends StagemonitorPlugin {

	private static final String ALERTING_PLUGIN_NAME = "Alerting";

	private final ConfigurationOption<Boolean> muteAlerts = ConfigurationOption.booleanOption()
			.key("stagemonitor.alerts.mute")
			.dynamic(true)
			.label("Mute alerts")
			.description("If set to `true`, alerts will be muted.")
			.defaultValue(false)
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {

	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Arrays.<ConfigurationOption<?>>asList(muteAlerts);
	}

	public boolean isMuteAlerts() {
		return muteAlerts.getValue();
	}
}
