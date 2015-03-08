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
import org.stagemonitor.core.configuration.Configuration;

public class AlerterFactory {

	private final AlertingPlugin alertingPlugin;
	private final Map<String, Alerter> alerterByType;
	private final LogAlerter logAlerter = new LogAlerter();

	public AlerterFactory(Configuration configuration) {
		this.alertingPlugin = configuration.getConfig(AlertingPlugin.class);
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
		Set<Alerter> alerters = new HashSet<Alerter>(alerterByType.size());
		if (alertingPlugin.isMuteAlerts()) {
			return alerters;
		}
		alerters.add(logAlerter);
		for (Subscription subscription : alertingPlugin.getSubscriptionsByIds().values()) {
			if (subscription.isAlertOn(incident.getNewStatus())) {
				alerters.add(alerterByType.get(subscription.getAlerterType()));
			}
		}
		return alerters;
	}
}
