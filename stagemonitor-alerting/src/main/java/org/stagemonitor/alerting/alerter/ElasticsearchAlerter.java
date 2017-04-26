package org.stagemonitor.alerting.alerter;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.util.StringUtils;

public class ElasticsearchAlerter extends Alerter {

	private CorePlugin corePlugin;
	private HttpClient httpClient;

	public ElasticsearchAlerter(ConfigurationRegistry configuration, HttpClient httpClient) {
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
		return !corePlugin.getElasticsearchUrls().isEmpty();
	}

	@Override
	public String getTargetLabel() {
		return "Index";
	}
}
