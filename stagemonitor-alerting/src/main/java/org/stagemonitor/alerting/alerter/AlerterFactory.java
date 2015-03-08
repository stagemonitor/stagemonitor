package org.stagemonitor.alerting.alerter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.incident.Incident;

public class AlerterFactory {

	private final AlertingPlugin alertingPlugin;
	private final Map<String, Alerter> alerterByType;

	public AlerterFactory(AlertingPlugin alertingPlugin) {
		this.alertingPlugin = alertingPlugin;
		Map<String, Alerter> alerters = new HashMap<String, Alerter>();
		for (Alerter alerter : ServiceLoader.load(Alerter.class)) {
			alerters.put(alerter.getAlerterType(), alerter);
		}
		alerterByType = Collections.unmodifiableMap(alerters);
	}

	public Collection<String> getAvailableAlerters() {
		ArrayList<String> alerterTypes = new ArrayList<String>(alerterByType.size());
		for (String alerterType : alerterByType.keySet()) {
			alerterTypes.add(alerterType);
		}
		Collections.sort(alerterTypes);
		return alerterTypes;
	}

	public Collection<Alerter> getAlerters(Check check, Incident incident) {
		if (alertingPlugin.isMuteAlerts()) {
			return Collections.emptyList();
		}
		Set<Alerter> alerters = new HashSet<Alerter>(alerterByType.size());
		for (Subscription subscription : alertingPlugin.getSubscriptionsByIds().values()) {
			if (subscription.isAlertOn(incident.getNewStatus())) {
				alerters.add(alerterByType.get(subscription.getAlerterType()));
			}
		}
		return alerters;
	}
}
