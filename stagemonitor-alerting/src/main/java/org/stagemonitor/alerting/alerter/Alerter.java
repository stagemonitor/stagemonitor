package org.stagemonitor.alerting.alerter;

import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.configuration.ConfigurationRegistry;

/**
 * An alerter reports incidents to some channel like email.
 * <p/>
 * To add a custom {@link Alerter}, just implement the abstract class and add the full qualified class name to
 * src/main/resources/META-INF/services/org.stagemonitor.alerting.alerter.Alerter.
 * The content of the file has to be the canonical class name of the alerter.
 */
public abstract class Alerter implements StagemonitorSPI {

	public void init(InitArguments initArguments) {
	}

	/**
	 * Triggers an alert
	 *
	 * @param alertArguments
	 */
	public abstract void alert(AlertArguments alertArguments);

	/**
	 * A unique name for this alerter e.g. email or irc.
	 *
	 * @return the unique alerter name
	 */
	public abstract String getAlerterType();

	/**
	 * An alerter is available if all required configuration options for the particular Alerter are set.
	 *
	 * @return <code>true</code>, if the alerter is available, <code>false</code> otherwise
	 */
	public boolean isAvailable() {
		return true;
	}

	/**
	 * The label of the target parameter that is used in {@link Subscription#target}
	 * <p/>
	 * For example "Email address" for the Email alerter. Returning <code>null</code> means that
	 * this alerter does not have a target parameter.
	 *
	 * @return The label of the target parameter that is used in {@link Subscription#target}
	 */
	public abstract String getTargetLabel();

	public static class InitArguments {
		private final ConfigurationRegistry configuration;

		InitArguments(ConfigurationRegistry configuration) {
			this.configuration = configuration;
		}

		public ConfigurationRegistry getConfiguration() {
			return configuration;
		}
	}

	public static class AlertArguments {
		private final Incident incident;
		private final Subscription subscription;

		/**
		 * @param incident     the incident to report
		 * @param subscription the corresponding subscription
		 */
		public AlertArguments(Incident incident, Subscription subscription) {
			this.incident = incident;
			this.subscription = subscription;
		}

		public Incident getIncident() {
			return incident;
		}

		public Subscription getSubscription() {
			return subscription;
		}
	}
}
