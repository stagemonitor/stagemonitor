package org.stagemonitor.alerting;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.alerting.alerter.AlerterFactory;
import org.stagemonitor.alerting.alerter.Subscription;
import org.stagemonitor.alerting.check.CheckGroup;
import org.stagemonitor.alerting.incident.ConcurrentMapIncidentRepository;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
	private final ConfigurationOption<Long> checkFrequency = ConfigurationOption.longOption()
			.key("stagemonitor.alerts.frequency")
			.dynamic(false)
			.label("Threshold check frequency (sec)")
			.description("The threshold check frequency in seconds.")
			.defaultValue(60L)
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Subscription[]> subscriptions = ConfigurationOption.jsonOption(Subscription[].class)
			.key("stagemonitor.alerts.subscriptions")
			.dynamic(true)
			.label("Alert Subscriptions")
			.description("The alert subscriptions.")
			.defaultValue(new Subscription[]{})
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<CheckGroup[]> checkGroups = ConfigurationOption.jsonOption(CheckGroup[].class)
			.key("stagemonitor.alerts.checkGroups")
			.dynamic(true)
			.label("Check Groups")
			.description("The check groups that contain thresholds for metrics.")
			.defaultValue(new CheckGroup[]{})
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {
		final AlertingPlugin config = configuration.getConfig(AlertingPlugin.class);
		new ThresholdMonitoringReporter(metricRegistry, config, new AlerterFactory(config),
				new ConcurrentMapIncidentRepository(new ConcurrentHashMap<String, Incident>()),
				Stagemonitor.getMeasurementSession())
				.start(config.checkFrequency.getValue(), TimeUnit.SECONDS);
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Arrays.<ConfigurationOption<?>>asList(muteAlerts, checkFrequency, subscriptions);
	}

	public boolean isMuteAlerts() {
		return muteAlerts.getValue();
	}

	public List<Subscription> getSubscriptions() {
		return Arrays.asList(subscriptions.getValue());
	}

	public List<CheckGroup> getCheckGroups() {
		return Arrays.asList(checkGroups.getValue());
	}
}
