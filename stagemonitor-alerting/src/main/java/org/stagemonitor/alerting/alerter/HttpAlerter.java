package org.stagemonitor.alerting.alerter;

import org.stagemonitor.core.util.HttpClient;

public class HttpAlerter extends Alerter {

	protected final HttpClient httpClient;

	public HttpAlerter() {
		this.httpClient = new HttpClient();
	}

	@Override
	public void alert(AlertArguments alertArguments) {
		httpClient.sendAsJson("POST", alertArguments.getSubscription().getTarget(), alertArguments.getIncident());
	}

	@Override
	public String getAlerterType() {
		return "HTTP";
	}

	@Override
	public String getTargetLabel() {
		return "URL";
	}

}
