package org.stagemonitor.alerting.alerter;

import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.util.HttpClient;

public class HttpAlerter implements Alerter {

	protected final HttpClient httpClient;

	public HttpAlerter() {
		this.httpClient = new HttpClient();
	}

	@Override
	public void alert(Incident incident, Subscription subscription) {
		httpClient.sendAsJson("POST", subscription.getTarget(), incident);
	}

	@Override
	public String getAlerterType() {
		return "HTTP";
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public String getTargetLabel() {
		return "URL";
	}

}
