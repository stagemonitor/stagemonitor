package org.stagemonitor.alerting.alerter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.Stagemonitor;

/**
 * An alerter that writes incidents to the log
 */
public class LogAlerter implements Alerter {

	private final Logger logger;
	private final AlertTemplateProcessor alertTemplateProcessor;

	public LogAlerter() {
		this(Stagemonitor.getConfiguration(AlertingPlugin.class), LoggerFactory.getLogger(LogAlerter.class));
	}

	public LogAlerter(AlertingPlugin alertingPlugin, Logger logger) {
		this.logger = logger;
		this.alertTemplateProcessor = alertingPlugin.getAlertTemplateProcessor();
	}

	@Override
	public void alert(Incident incident, Subscription subscription) {
		String message = alertTemplateProcessor.processPlainTextTemplate(incident);
		switch (incident.getNewStatus()) {
			case CRITICAL:
			case ERROR:
				logger.error(message);
				break;
			default:
				logger.warn(message);
		}
	}

	@Override
	public String getAlerterType() {
		return "Log Alerts";
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public String getTargetLabel() {
		return null;
	}
}
