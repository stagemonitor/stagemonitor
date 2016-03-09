package org.stagemonitor.alerting.alerter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.core.Stagemonitor;

/**
 * An alerter that writes incidents to the log
 */
public class LogAlerter extends Alerter {

	private final Logger logger;
	private final AlertTemplateProcessor alertTemplateProcessor;

	public LogAlerter() {
		this(Stagemonitor.getPlugin(AlertingPlugin.class), LoggerFactory.getLogger(LogAlerter.class));
	}

	public LogAlerter(AlertingPlugin alertingPlugin, Logger logger) {
		this.logger = logger;
		this.alertTemplateProcessor = alertingPlugin.getAlertTemplateProcessor();
	}

	@Override
	public void alert(AlertArguments alertArguments) {
		String message = alertTemplateProcessor.processPlainTextTemplate(alertArguments.getIncident());
		switch (alertArguments.getIncident().getNewStatus()) {
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
	public String getTargetLabel() {
		return null;
	}
}
