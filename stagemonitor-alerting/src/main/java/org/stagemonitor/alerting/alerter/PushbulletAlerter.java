package org.stagemonitor.alerting.alerter;

import java.util.HashMap;
import java.util.Map;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.util.StringUtils;

public class PushbulletAlerter extends Alerter {

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
	public void alert(AlertArguments alertArguments) {
		sendPushbulletNotification(alertArguments.getSubscription().getTarget(),
				alertTemplateProcessor.processShortDescriptionTemplate(alertArguments.getIncident()),
				alertTemplateProcessor.processPlainTextTemplate(alertArguments.getIncident()));
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
