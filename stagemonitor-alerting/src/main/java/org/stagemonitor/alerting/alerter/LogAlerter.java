package org.stagemonitor.alerting.alerter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.incident.Incident;

/**
 * An alerter that writes incidents to the log
 */
public class LogAlerter implements Alerter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void alert(Incident incident) {
		switch (incident.getNewStatus()) {
			case CRITICAL:
			case ERROR:
				logger.error(incident.toString());
				break;
			default:
				logger.warn(incident.toString());
		}
	}

	@Override
	public String getAlerterType() {
		return "Log";
	}
}
