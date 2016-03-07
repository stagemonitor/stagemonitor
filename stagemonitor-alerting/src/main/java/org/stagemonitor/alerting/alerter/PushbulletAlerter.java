package org.stagemonitor.alerting.alerter;

import java.util.HashMap;
import java.util.Map;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.StringUtils;

public class PushbulletAlerter implements Alerter {

	private final AlertTemplateProcessor alertTemplateProcessor;
	private final AlertingPlugin alertingPlugin;

	public PushbulletAlerter() {
		this(Stagemonitor.getPlugin(AlertingPlugin.class));
	}

	public PushbulletAlerter(AlertingPlugin alertingPlugin) {
		this.alertingPlugin = alertingPlugin;
		this.alertTemplateProcessor = this.alertingPlugin.getAlertTemplateProcessor();
	}

	@Override
	public void alert(Incident incident, Subscription subscription) {
		sendPushbulletNotification(subscription.getTarget(),
				alertTemplateProcessor.processShortDescriptionTemplate(incident),
				alertTemplateProcessor.processPlainTextTemplate(incident));
	}

	public void sendPushbulletNotification(String channelTag, String subject, String content) {
		HttpClient client = new HttpClient();
		PushbulletNotification notification = new PushbulletNotification(subject, content, channelTag);
		Map<String, String> authorizationHeader = new HashMap<String, String>();
		authorizationHeader.put("Authorization", "Bearer " + alertingPlugin.getPushbulletAccessToken());
		client.sendAsJson("POST", "https://api.pushbullet.com/v2/pushes", notification, authorizationHeader);
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
