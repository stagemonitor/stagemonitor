package org.stagemonitor.alerting.alerter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.configuration.Configuration;

public class AlertSender {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final AlertingPlugin alertingPlugin;
	private final Map<String, Alerter> alerterByType;
	private final LogAlerter logAlerter = new LogAlerter();

	public AlertSender(Configuration configuration) {
		this(configuration, ServiceLoader.load(Alerter.class));
	}

	public AlertSender(Configuration configuration, Iterable<Alerter> alerterIterable) {
		this.alertingPlugin = configuration.getConfig(AlertingPlugin.class);
		Map<String, Alerter> alerters = new HashMap<String, Alerter>();
		for (Alerter alerter : alerterIterable) {
			alerters.put(alerter.getAlerterType(), alerter);
		}
		alerterByType = Collections.unmodifiableMap(alerters);
	}

	public Collection<String> getAvailableAlerters() {
		ArrayList<String> alerterTypes = new ArrayList<String>(alerterByType.size());
		for (Map.Entry<String, Alerter> entry : alerterByType.entrySet()) {
			if (entry.getValue().isAvailable()) {
				alerterTypes.add(entry.getKey());
			}
		}
		Collections.sort(alerterTypes);
		return alerterTypes;
	}

	public void sendAlerts(Check check, Incident incident) {
		if (alertingPlugin.isMuteAlerts() || ! incident.isAlertIncident(check)) {
			return;
		}
		logAlerter.alert(incident, null);
		for (Subscription subscription : alertingPlugin.getSubscriptionsByIds().values()) {
			if (subscription.isAlertOn(incident.getNewStatus())) {
				Alerter alerter = alerterByType.get(subscription.getAlerterType());
				if (alerter != null && alerter.isAvailable()) {
					alerter.alert(incident, subscription);
				} else {
					logger.warn("Alerter with type '{}' is not available. " +
							"Either the name of the alerter is invalid or it is not configured correctly.",
							subscription.getAlerterType());
				}
			}
		}
	}

}
