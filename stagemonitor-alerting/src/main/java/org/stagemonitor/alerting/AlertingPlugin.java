package org.stagemonitor.alerting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import org.stagemonitor.alerting.alerter.AlerterFactory;
import org.stagemonitor.alerting.alerter.Subscription;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.incident.ConcurrentMapIncidentRepository;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.Stagemonitor;
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
	private final ConfigurationOption<Long> checkFrequency = ConfigurationOption.longOption()
			.key("stagemonitor.alerts.frequency")
			.dynamic(false)
			.label("Threshold check frequency (sec)")
			.description("The threshold check frequency in seconds.")
			.defaultValue(60L)
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	public final ConfigurationOption<Map<String, Subscription>> subscriptions = ConfigurationOption
			.jsonOption(new TypeReference<Map<String, Subscription>>() {}, Map.class)
			.key("stagemonitor.alerts.subscriptions")
			.dynamic(true)
			.label("Alert Subscriptions")
			.description("The alert subscriptions.")
			.defaultValue(Collections.<String, Subscription>emptyMap())
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private final ConfigurationOption<Map<String, Check>> checks = ConfigurationOption
			.jsonOption(new TypeReference<Map<String, Check>>() {}, Map.class)
			.key("stagemonitor.alerts.checks")
			.dynamic(true)
			.label("Check Groups")
			.description("The check groups that contain thresholds for metrics.")
			.defaultValue(Collections.<String, Check>emptyMap())
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private static AlerterFactory alerterFactory;
	private static IncidentRepository incidentRepository;

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {
		final AlertingPlugin config = configuration.getConfig(AlertingPlugin.class);
		// TODO make configurable
		alerterFactory = new AlerterFactory(config);
		incidentRepository = new ConcurrentMapIncidentRepository(new ConcurrentHashMap<String, Incident>());
		new ThresholdMonitoringReporter(metricRegistry, config, alerterFactory, incidentRepository, Stagemonitor.getMeasurementSession())
				.start(config.checkFrequency.getValue(), TimeUnit.SECONDS);
	}

	@Override
	public List<ConfigurationOption<?>> getConfigurationOptions() {
		return Arrays.<ConfigurationOption<?>>asList(muteAlerts, checkFrequency, subscriptions, checks);
	}

	public boolean isMuteAlerts() {
		return muteAlerts.getValue();
	}

	public Map<String, Subscription> getSubscriptionsByIds() {
		return subscriptions.getValue();
	}

	public String getSubscriptionsByIdsAsJson() {
		return subscriptions.getValueAsString();
	}

	public Map<String, Check> getChecks() {
		return checks.getValue();
	}

	public IncidentRepository getIncidentRepository() {
		return incidentRepository;
	}

	public AlerterFactory getAlerterFactory() {
		return alerterFactory;
	}
}
