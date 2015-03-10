package org.stagemonitor.alerting.alerter;

import org.stagemonitor.alerting.incident.Incident;

/**
 * An alerter reports incidents to some chanel like email.
 * <p/>
 * To add a custom {@link Alerter}, just implement the interface and create a file under
 * src/main/resources/META-INF/services/org.stagemonitor.alerting.alerter.Alerter.
 * The content of the file has to be the canonical class name of the alerter.
 */
public interface Alerter {

	/**
	 * Triggers an alert
	 *
	 * @param incident the incident to report
	 * @param subscription the corresponding subscription
	 */
	void alert(Incident incident, Subscription subscription);

	/**
	 * A unique name for this alerter e.g. email or irc.
	 *
	 * @return the unique alerter name
	 */
	String getAlerterType();

	/**
	 * An alerter is available, if all required configuration options for the particular Alerter are set.
	 *
	 * @return <code>true</code>, if the alerter is available, <code>false</code> otherwise
	 */
	boolean isAvailable();

}
