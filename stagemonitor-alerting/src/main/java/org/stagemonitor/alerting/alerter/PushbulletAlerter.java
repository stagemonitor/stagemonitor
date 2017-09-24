package org.stagemonitor.alerting.alerter;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.http.HttpRequestBuilder;
import org.stagemonitor.util.StringUtils;

public class PushbulletAlerter extends Alerter {

	private final AlertTemplateProcessor alertTemplateProcessor;
	private final AlertingPlugin alertingPlugin;
	private final HttpClient httpClient;

	public PushbulletAlerter() {
		this(Stagemonitor.getPlugin(AlertingPlugin.class), new HttpClient());
	}

	public PushbulletAlerter(AlertingPlugin alertingPlugin, HttpClient httpClient) {
		this.alertingPlugin = alertingPlugin;
		this.alertTemplateProcessor = this.alertingPlugin.getAlertTemplateProcessor();
		this.httpClient = httpClient;
	}

	@Override
	public void alert(AlertArguments alertArguments) {
		sendPushbulletNotification(alertArguments.getSubscription().getTarget(),
				alertTemplateProcessor.processShortDescriptionTemplate(alertArguments.getIncident()),
				alertTemplateProcessor.processPlainTextTemplate(alertArguments.getIncident()));
	}

	private void sendPushbulletNotification(String channelTag, String subject, String content) {
		PushbulletNotification notification = new PushbulletNotification(subject, content, channelTag);
		httpClient.send(HttpRequestBuilder.<Integer>jsonRequest("POST", "https://api.pushbullet.com/v2/pushes", notification)
				.addHeader("Authorization", "Bearer " + alertingPlugin.getPushbulletAccessToken())
				.build());
	}

	@Override
	public String getAlerterType() {
		return "Pushbullet";
	}

	@Override
	public boolean isAvailable() {
		return StringUtils.isNotEmpty(alertingPlugin.getPushbulletAccessToken());
	}

	@Override
	public String getTargetLabel() {
		return "Channel Tag";
	}
}
