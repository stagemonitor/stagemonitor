package org.stagemonitor.alerting.alerter;

import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.StringUtils;

public class ElasticsearchAlerter implements Alerter {

	private CorePlugin corePlugin;
	private HttpClient httpClient;

	public ElasticsearchAlerter() {
		this(Stagemonitor.getConfiguration(), new HttpClient());
	}

	public ElasticsearchAlerter(Configuration configuration, HttpClient httpClient) {
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.httpClient = httpClient;
	}

	@Override
	public void alert(Incident incident, Subscription subscription) {
		String target = subscription.getTarget();
		if (StringUtils.isEmpty(target)) {
			target = "/stagemonitor/alerts";
		}
		httpClient.sendAsJson("POST", corePlugin.getElasticsearchUrl() + target, incident);
	}

	@Override
	public String getAlerterType() {
		return "Elasticsearch";
	}

	@Override
	public boolean isAvailable() {
		return StringUtils.isNotEmpty(corePlugin.getElasticsearchUrl());
	}
}
