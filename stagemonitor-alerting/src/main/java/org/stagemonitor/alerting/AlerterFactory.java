package org.stagemonitor.alerting;

import java.util.Collections;
import java.util.List;

public class AlerterFactory {

	private AlertingPlugin alertingPlugin;

	public AlerterFactory(AlertingPlugin alertingPlugin) {
		this.alertingPlugin = alertingPlugin;
	}

	public List<Alerter> getAlerters(Incident incident) {
		if (alertingPlugin.isMuteAlerts()) {
			return Collections.emptyList();
		}
		// TODO
		return Collections.emptyList();
	}
}
