package org.stagemonitor.alerting;

import org.stagemonitor.alerting.incident.Incident;

public interface Alerter {

	void alert(Incident incident);

	String getAlerterName();

}
