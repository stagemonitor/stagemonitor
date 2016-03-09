package org.stagemonitor.alerting.alerter;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.StringUtils;

public class ElasticsearchAlerter extends Alerter {

	private CorePlugin corePlugin;
	private HttpClient httpClient;

	public ElasticsearchAlerter(Configuration configuration, HttpClient httpClient) {
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.httpClient = httpClient;
	}

	@Override
	public void alert(AlertArguments alertArguments) {
		String target = alertArguments.getSubscription().getTarget();
		if (StringUtils.isEmpty(target)) {
			target = "/stagemonitor/alerts";
		}
		httpClient.sendAsJson("POST", corePlugin.getElasticsearchUrl() + target, alertArguments.getIncident());
	}

	@Override
	public String getAlerterType() {
		return "Elasticsearch";
	}

	@Override
	public boolean isAvailable() {
		return StringUtils.isNotEmpty(corePlugin.getElasticsearchUrl());
	}

	@Override
	public String getTargetLabel() {
		return "Index";
	}
}
