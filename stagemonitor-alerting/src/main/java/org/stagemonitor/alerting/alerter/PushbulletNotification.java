package org.stagemonitor.alerting.alerter;

public class PushbulletNotification {

	private String type = "note";
	private String title;
	private String body;

	public PushbulletNotification(String title, String body) {
		this.title = title;
		this.body = body;
	}

	public String getBody() {
		return body;
	}

	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}
}
