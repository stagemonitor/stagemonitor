package org.stagemonitor.alerting;

public interface Alerter {

	void alert(Incident incident);

	String getAlerterName();

}
