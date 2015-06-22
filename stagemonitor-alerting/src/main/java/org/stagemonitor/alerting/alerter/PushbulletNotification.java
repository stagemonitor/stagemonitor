package org.stagemonitor.alerting.alerter;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PushbulletNotification {

	private final String title;
	private final String body;
	@JsonProperty("channel_tag")
	private final String channelTag;

	public PushbulletNotification(String title, String body, String channelTag) {
		this.title = title;
		this.body = body;
		this.channelTag = channelTag;
	}

	public String getBody() {
		return body;
	}

	public String getTitle() {
		return title;
	}

	public String getType() {
		return "note";
	}

	public String getChannelTag() {
		return channelTag;
	}
}
