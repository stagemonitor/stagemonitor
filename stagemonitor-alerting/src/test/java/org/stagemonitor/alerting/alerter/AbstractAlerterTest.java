package org.stagemonitor.alerting.alerter;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.configuration.source.SimpleSource;
import org.stagemonitor.core.CorePlugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

public class AbstractAlerterTest {

	protected SimpleSource configurationSource;
	protected ConfigurationRegistry configuration;
	protected AlertingPlugin alertingPlugin;

	public AbstractAlerterTest() {
		configurationSource = new SimpleSource();
		alertingPlugin = new AlertingPlugin();
		configuration = new ConfigurationRegistry(Arrays.asList(new CorePlugin(), alertingPlugin), Collections.singletonList(configurationSource));
	}

	public AlertSender createAlertSender(Alerter alerter) {
		return new AlertSender(configuration, Collections.singletonList(alerter));
	}

	public Subscription createSubscription(Alerter forAlerter) {
		Subscription subscription = new Subscription();
		subscription.setAlerterType(forAlerter.getAlerterType());
		subscription.setAlertOnBackToOk(true);
		subscription.setAlertOnWarn(true);
		subscription.setAlertOnError(true);
		subscription.setAlertOnCritical(true);
		return subscription;
	}

	public String toFreemarkerIsoLocal(Date date) {
		TimeZone tz = TimeZone.getDefault();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		df.setTimeZone(tz);
		return df.format(date);
	}

}
