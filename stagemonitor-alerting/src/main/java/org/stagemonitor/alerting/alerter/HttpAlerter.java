package org.stagemonitor.alerting.alerter;

import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.http.HttpRequestBuilder;

public class HttpAlerter extends Alerter {

	protected final HttpClient httpClient;

	public HttpAlerter() {
		this.httpClient = new HttpClient();
	}

	@Override
	public void alert(AlertArguments alertArguments) {
		httpClient.send(HttpRequestBuilder.<Integer>jsonRequest("POST", alertArguments.getSubscription().getTarget(), alertArguments.getIncident()).build());
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
