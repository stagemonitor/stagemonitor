package org.stagemonitor.alerting;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.alerter.AlertSender;
import org.stagemonitor.alerting.alerter.AlertTemplateProcessor;
import org.stagemonitor.alerting.alerter.AlerterTypeServlet;
import org.stagemonitor.alerting.alerter.IncidentServlet;
import org.stagemonitor.alerting.alerter.Subscription;
import org.stagemonitor.alerting.alerter.TestAlertSenderServlet;
import org.stagemonitor.alerting.annotation.SlaCheckCreatingClassPathScanner;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.incident.ConcurrentMapIncidentRepository;
import org.stagemonitor.alerting.incident.ElasticsearchIncidentRepository;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.configuration.ConfigurationOption;
import org.stagemonitor.core.configuration.source.SimpleSource;

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
	private final ConfigurationOption<Map<String, Subscription>> subscriptions = ConfigurationOption
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
			.sensitive()
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
	private ConfigurationOption<String> pushbulletAccessToken = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.pushbullet.accessToken")
			.dynamic(true)
			.label("Pushbullet Access Token")
			.description("The Access Token for the Pushbullet API. You can find it in your Pushbullet account settings.")
			.defaultValue(null)
			.configurationCategory(ALERTING_PLUGIN_NAME)
			.sensitive()
			.build();
	private ConfigurationOption<String> htmlAlertTemplate = ConfigurationOption.stringOption()
			.key("stagemonitor.alerts.template.html")
			.dynamic(true)
			.label("Alerts HTML freemarker template")
			.defaultValue("<#-- @ftlvariable name=\"incident\" type=\"org.stagemonitor.alerting.incident.Incident\" -->\n" +
					"<h3>Incident for check ${incident.checkName}</h3>\n" +
					"First failure: ${incident.firstFailureAt?datetime?iso_local}<br>\n" +
					"<#if incident.resolvedAt??>\n" +
					"Resolved at: ${incident.resolvedAt?datetime?iso_local}<br>\n" +
					"</#if>\n" +
					"Old status: ${incident.oldStatus!'OK'}<br>\n" +
					"New status: ${incident.newStatus}<br>\n" +
					"<#if incident.failedChecks gt 0>" +
					"Failing check<#if incident.failedChecks gt 1>s</#if>: ${incident.failedChecks}<br>\n" +
					"Hosts: ${incident.hosts?join(\", \")}<br>\n" +
					"Instances: ${incident.instances?join(\", \")}<br><br>\n" +
					"\n" +
					"	<#if incident.checkResults?has_content>\n" +
					"<table>\n" +
					"	<thead>\n" +
					"	<tr>\n" +
					"		<th>Host</th>\n" +
					"		<th>Instance</th>\n" +
					"		<th>Status</th>\n" +
					"		<th>Description</th>\n" +
					"		<th>Current Value</th>\n" +
					"	</tr>\n" +
					"	</thead>\n" +
					"	<tbody>\n" +
					"		<#list incident.checkResults as results>\n" +
					"			<#assign measurementSession=results.measurementSession/>\n" +
					"			<#list results.getResults() as result>\n" +
					"			<tr>\n" +
					"				<td>${measurementSession.hostName}</td>\n" +
					"				<td>${measurementSession.instanceName}</td>\n" +
					"				<td>${result.status}</td>\n" +
					"				<td>${result.failingExpression}</td>\n" +
					"				<td>${result.currentValue}</td>\n" +
					"			</tr>\n" +
					"			</#list>\n" +
					"		</#list>\n" +
					"	</tbody>\n" +
					"</table>\n" +
					"	</#if>\n" +
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
					"<#if incident.failedChecks gt 0>" +
					"Failing check<#if incident.failedChecks gt 1>s</#if>: ${incident.failedChecks}\n" +
					"Hosts: ${incident.hosts?join(\", \")}\n" +
					"Instances: ${incident.instances?join(\", \")}\n" +
					"\n" +
					"Details:" +
					"\n" +
					"<#list incident.checkResults as results>\n" +
					"<#assign measurementSession=results.measurementSession/>\n" +
					"<#list results.getResults() as result>\n" +
					"Host:			${measurementSession.hostName}\n" +
					"Instance:		${measurementSession.instanceName}\n" +
					"Status: 		${result.status}\n" +
					"Description:	${result.failingExpression}\n" +
					"Current value:	${result.currentValue}\n" +
					"\n" +
					"</#list>" +
					"</#list>" +
					"</#if>")
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
	private ThresholdMonitoringReporter thresholdMonitoringReporter;

	@Override
	public void initializePlugin(StagemonitorPlugin.InitArguments initArguments) throws Exception {
		final AlertingPlugin alertingPlugin = initArguments.getPlugin(AlertingPlugin.class);
		alertSender = new AlertSender(initArguments.getConfiguration());
		CorePlugin corePlugin = initArguments.getPlugin(CorePlugin.class);
		if (corePlugin.getElasticsearchUrl() != null) {
			incidentRepository = new ElasticsearchIncidentRepository(corePlugin.getElasticsearchClient());
		} else {
			incidentRepository = new ConcurrentMapIncidentRepository();
		}
		logger.info("Using {} for storing incidents.", incidentRepository.getClass().getSimpleName());

		thresholdMonitoringReporter = new ThresholdMonitoringReporter(initArguments.getMetricRegistry(), alertingPlugin, alertSender, incidentRepository, initArguments.getMeasurementSession());
		thresholdMonitoringReporter.start(alertingPlugin.checkFrequency.getValue(), TimeUnit.SECONDS);
		SlaCheckCreatingClassPathScanner.onStart(initArguments.getMeasurementSession());
	}

	@Override
	public void onShutDown() {
		thresholdMonitoringReporter.close();
	}

	@Override
	public void registerWidgetTabPlugins(WidgetTabPluginsRegistry widgetTabPluginsRegistry) {
		widgetTabPluginsRegistry.addWidgetTabPlugin("/stagemonitor/static/tabs/alert/alerting-tab");
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

	public void addCheck(Check check) throws IOException {
		final LinkedHashMap<String, Check> newValue = new LinkedHashMap<String, Check>(getChecks());
		newValue.put(check.getId(), check);
		checks.update(newValue, SimpleSource.NAME);
	}

	public ThresholdMonitoringReporter getThresholdMonitoringReporter() {
		return thresholdMonitoringReporter;
	}

	public IncidentRepository getIncidentRepository() {
		return incidentRepository;
	}

	/**
	 * Returns the alert sender. It may be null if the alerting plugin has not yet been initialized.
	 *
	 * @return the alert sender or <code>null</code>
	 */
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

	public String getPushbulletAccessToken() {
		return pushbulletAccessToken.getValue();
	}

	public static class Initializer implements ServletContainerInitializer {

		@Override
		public void onStartup(Set<Class<?>> c, ServletContext ctx) {
			final AlertingPlugin alertingPlugin = Stagemonitor.getPlugin(AlertingPlugin.class);
			ctx.addServlet(AlerterTypeServlet.class.getSimpleName(), new AlerterTypeServlet(alertingPlugin, Stagemonitor.getMeasurementSession()))
					.addMapping("/stagemonitor/alerter-types");

			ctx.addServlet(IncidentServlet.class.getSimpleName(), new IncidentServlet(alertingPlugin))
					.addMapping("/stagemonitor/incidents");

			ctx.addServlet(TestAlertSenderServlet.class.getSimpleName(), new TestAlertSenderServlet())
					.addMapping("/stagemonitor/test-alert");
		}
	}

}
