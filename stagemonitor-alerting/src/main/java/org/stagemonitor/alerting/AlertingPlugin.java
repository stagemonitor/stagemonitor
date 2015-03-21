package org.stagemonitor.alerting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.alerter.AlertSender;
import org.stagemonitor.alerting.alerter.AlertTemplateProcessor;
import org.stagemonitor.alerting.alerter.Subscription;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.incident.ConcurrentMapIncidentRepository;
import org.stagemonitor.alerting.incident.ElasticsearchIncidentRepository;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOption;

public class AlertingPlugin extends StagemonitorPlugin {

	private final Logger logger = LoggerFactory.getLogger(getClass());

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
	public final ConfigurationOption<Map<String, Check>> checks = ConfigurationOption
			.jsonOption(new TypeReference<Map<String, Check>>() {}, Map.class)
			.key("stagemonitor.alerts.checks")
			.dynamic(true)
			.label("Check Groups")
			.description("The check groups that contain thresholds for metrics.")
			.defaultValue(Collections.<String, Check>emptyMap())
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();

	private ConfigurationOption<String> smtpHost = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.smtp.host")
			.dynamic(true)
			.label("SMTP Mailhost")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private ConfigurationOption<String> smtpPassword = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.smtp.password")
			.dynamic(true)
			.label("SMTP-Password")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private ConfigurationOption<String> smtpUser = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.smtp.user")
			.dynamic(true)
			.label("SMTP-User name")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private ConfigurationOption<String> smtpProtocol = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.smtp.protocol")
			.dynamic(true)
			.defaultValue("smtp")
			.label("SMTP-Protocol")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private ConfigurationOption<Integer> smtpPort = ConfigurationOption.integerOption()
			.key("stagemonitor.alerts.smtp.port")
			.dynamic(true)
			.defaultValue(25)
			.label("SMTP-Port")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private ConfigurationOption<String> smtpFrom = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.smtp.from")
			.dynamic(true)
			.label("SMTP-From")
			.description("The from email address for sending notifications.")
			.defaultValue("alert@stagemonitor.org")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();

	private ConfigurationOption<String> htmlAlertTemplate = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.template.html")
			.dynamic(true)
			.label("Alerts HTML freemarker template")
			.defaultValue("<#-- @ftlvariable name=\"incident\" type=\"org.stagemonitor.alerting.incident.Incident\" -->\n" +
					"<html>\n" +
					"<head>\n" +
					"\n" +
					"</head>\n" +
					"<body>\n" +
					"<h3>Incident for check ${incident.checkName}</h3>\n" +
					"First failure: ${incident.firstFailureAt?datetime?iso_local}<br>\n" +
					"<#if incident.resolvedAt??>\n" +
					"Resolved at: ${incident.resolvedAt?datetime?iso_local}<br>\n" +
					"</#if>\n" +
					"Old status: ${incident.oldStatus!'OK'}<br>\n" +
					"New status: ${incident.newStatus}<br>\n" +
					"Failing check<#if incident.failedChecks gt 1>s</#if>: ${incident.failedChecks}<br>\n" +
					"Hosts: ${incident.hosts?join(\", \")}<br>\n" +
					"Instances: ${incident.instances?join(\", \")}<br><br>\n" +
					"\n" +
					"<#if incident.checkResults?has_content>\n" +
					"<table>\n" +
					"\t<thead>\n" +
					"\t<tr>\n" +
					"\t\t<th>Host</th>\n" +
					"\t\t<th>Instance</th>\n" +
					"\t\t<th>Status</th>\n" +
					"\t\t<th>Description</th>\n" +
					"\t\t<th>Current Value</th>\n" +
					"\t</tr>\n" +
					"\t</thead>\n" +
					"\t<tbody>\n" +
					"\t\t<#list incident.checkResults as results>\n" +
					"\t\t\t<#assign measurementSession=results.measurementSession/>\n" +
					"\t\t\t<#list results.getResults() as result>\n" +
					"\t\t\t<tr>\n" +
					"\t\t\t\t<td>${measurementSession.hostName}</td>\n" +
					"\t\t\t\t<td>${measurementSession.instanceName}</td>\n" +
					"\t\t\t\t<td>${result.status}</td>\n" +
					"\t\t\t\t<td>${result.failingExpression}</td>\n" +
					"\t\t\t\t<td>${result.currentValue}</td>\n" +
					"\t\t\t</tr>\n" +
					"\t\t\t</#list>\n" +
					"\t\t</#list>\n" +
					"\t</tbody>\n" +
					"</table>\n" +
					"</body>\n" +
					"</html>\n" +
					"</#if>\n")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private ConfigurationOption<String> plainTextAlertTemplate = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.template.plainText")
			.dynamic(true)
			.label("Alerts plain text freemarker template")
			.defaultValue("<#-- @ftlvariable name=\"incident\" type=\"org.stagemonitor.alerting.incident.Incident\" -->\n" +
					"Incident for check '${incident.checkName}':\n" +
					"First failure: ${incident.firstFailureAt?datetime?iso_local}\n" +
					"<#if incident.resolvedAt??>\n" +
					"Resolved at: ${incident.resolvedAt?datetime?iso_local}\n" +
					"</#if>\n" +
					"Old status: ${incident.oldStatus!'OK'}\n" +
					"New status: ${incident.newStatus}\n" +
					"Failing check<#if incident.failedChecks gt 1>s</#if>: ${incident.failedChecks}\n" +
					"Hosts: ${incident.hosts?join(\", \")}\n" +
					"Instances: ${incident.instances?join(\", \")}\n" +
					"\n" +
					"host|instance|status|description|current value\n" +
					"----|--------|------|-----------|-------------\n" +
					"<#list incident.checkResults as results>\n" +
					"\t<#assign measurementSession=results.measurementSession/>\n" +
					"\t<#list results.getResults() as result>\n" +
					"${measurementSession.hostName} | ${measurementSession.instanceName} | ${result.status} | ${result.failingExpression} | ${result.currentValue}\n" +
					"\t</#list>\n" +
					"</#list>")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();
	private ConfigurationOption<String> shortDescriptionAlertTemplate = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.template.shortDescription")
			.dynamic(true)
			.label("Alerts short description freemarker template")
			.description("Used for example for the email subject.")
			.defaultValue("[${incident.oldStatus!'OK'} -> ${incident.newStatus}] ${incident.checkName} has ${incident.failedChecks} failing check<#if incident.failedChecks gt 1>s</#if>")
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.build();

	private AlertSender alertSender;
	private IncidentRepository incidentRepository;
	private AlertTemplateProcessor alertTemplateProcessor;

	@Override
	public void initializePlugin(MetricRegistry metricRegistry, Configuration configuration) throws Exception {
		final AlertingPlugin alertingPlugin = configuration.getConfig(AlertingPlugin.class);
		alertSender = new AlertSender(configuration);
		CorePlugin corePlugin = configuration.getConfig(CorePlugin.class);
		if (corePlugin.getElasticsearchUrl() != null) {
			incidentRepository = new ElasticsearchIncidentRepository(corePlugin.getElasticsearchClient());
		} else {
			incidentRepository = new ConcurrentMapIncidentRepository();
		}
		logger.info("Using {} for storing incidents.", incidentRepository.getClass().getSimpleName());

		new ThresholdMonitoringReporter(metricRegistry, alertingPlugin, alertSender, incidentRepository, Stagemonitor.getMeasurementSession())
				.start(alertingPlugin.checkFrequency.getValue(), TimeUnit.SECONDS);
	}

	@Override
	public List<String> getPathsOfWidgetTabPlugins() {
		return Arrays.asList("/stagemonitor/static/tabs/alert/alerting-tab");
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

	public AlertSender getAlertSender() {
		return alertSender;
	}

	public String getChecksAsJson() {
		return checks.getValueAsString();
	}

	public String getSmtpHost() {
		return smtpHost.getValue();
	}

	public String getSmtpPassword() {
		return smtpPassword.getValue();
	}

	public String getSmtpUser() {
		return smtpUser.getValue();
	}

	public String getSmtpProtocol() {
		return smtpProtocol.getValue();
	}

	public int getSmtpPort() {
		return smtpPort.getValue();
	}

	public String getSmtpFrom() {
		return smtpFrom.getValue();
	}

	public String getHtmlAlertTemplate() {
		return htmlAlertTemplate.getValue();
	}

	public String getPlainTextAlertTemplate() {
		return plainTextAlertTemplate.getValue();
	}

	public String getShortDescriptionAlertTemplate() {
		return shortDescriptionAlertTemplate.getValue();
	}

	public AlertTemplateProcessor getAlertTemplateProcessor() {
		if (alertTemplateProcessor == null) {
			alertTemplateProcessor = new AlertTemplateProcessor(this);
		}
		return alertTemplateProcessor;
	}

}
