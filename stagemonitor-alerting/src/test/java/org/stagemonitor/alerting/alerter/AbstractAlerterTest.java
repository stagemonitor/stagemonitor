package org.stagemonitor.alerting.alerter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;

public abstract class AbstractAlerterTest {

	protected SimpleSource configurationSource;
	protected Configuration configuration;
	protected AlertingPlugin alertingPlugin;

	public AbstractAlerterTest() {
		configurationSource = new SimpleSource();
		alertingPlugin = new AlertingPlugin();
		configuration = new Configuration(Arrays.asList(alertingPlugin), Arrays.<ConfigurationSource>asList(configurationSource), null);
	}

	public AlertSender createAlertSender(Alerter alerter) {
		return new AlertSender(configuration, Arrays.asList(alerter));
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
