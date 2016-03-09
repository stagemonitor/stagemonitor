package org.stagemonitor.alerting.alerter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.alerting.check.MetricCategory;
import org.stagemonitor.alerting.check.Threshold;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.HttpClient;

public class AlertSender {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final AlertingPlugin alertingPlugin;
	private final Map<String, Alerter> alerterByType;
	private final List<Alerter> defaultAlerters;

	public AlertSender(Configuration configuration) {
		this(configuration, ServiceLoader.load(Alerter.class));
	}

	public AlertSender(Configuration configuration, Iterable<Alerter> alerterIterable) {
		this.alertingPlugin = configuration.getConfig(AlertingPlugin.class);
		Map<String, Alerter> alerters = new HashMap<String, Alerter>();
		for (Alerter alerter : alerterIterable) {
			alerter.init(new Alerter.InitArguments(configuration));
			alerters.put(alerter.getAlerterType(), alerter);
		}
		alerterByType = Collections.unmodifiableMap(alerters);
		defaultAlerters = Arrays.asList(new LogAlerter(), new ElasticsearchAlerter(configuration, new HttpClient()));
	}

	/**
	 * Returns all available {@link Alerter}s
	 *
	 * @return all available {@link Alerter}s
	 */
	public List<Alerter> getAvailableAlerters() {
		List<Alerter> alerters = new ArrayList<Alerter>(alerterByType.size());
		for (Alerter alerter : alerterByType.values()) {
			if (alerter.isAvailable()) {
				alerters.add(alerter);
			}
		}
		return alerters;
	}

	public Incident sendTestAlert(Subscription subscription, CheckResult.Status status) {
		Check check = new Check();
		check.setName("Test Check");
		check.setApplication("testApp");
		check.setTarget(Pattern.compile("test"));
		check.setMetricCategory(MetricCategory.TIMER);
		check.getWarn().add(new Threshold("mean", Threshold.Operator.GREATER_EQUAL, 1));

		Incident testIncident = new Incident(check, new MeasurementSession("testApp", "testHost", "testInstance"),
				Arrays.asList(new CheckResult("test", 10, status)));

		tryAlert(testIncident, subscription, alerterByType.get(subscription.getAlerterType()));
		return testIncident;
	}

	public void sendAlerts(Check check, Incident incident) {
		if (alertingPlugin.isMuteAlerts() || !incident.isAlertIncident(check)) {
			return;
		}
		for (Alerter alerter : defaultAlerters) {
			tryAlert(incident, null, alerter);
		}
		for (Subscription subscription : alertingPlugin.getSubscriptionsByIds().values()) {
			if (subscription.isAlertOn(incident.getNewStatus())) {
				tryAlert(incident, subscription, alerterByType.get(subscription.getAlerterType()));
			}
		}
	}

	private void tryAlert(Incident incident, Subscription subscription, Alerter alerter) {
		if (alerter != null && alerter.isAvailable()) {
			try {
				alerter.alert(new Alerter.AlertArguments(incident, subscription));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			if (subscription != null) {
				logger.warn("Alerter with type '{}' is not available. " +
						"Either the name of the alerter is invalid or it is not configured correctly.",
						subscription.getAlerterType());
			}
		}
	}

}
