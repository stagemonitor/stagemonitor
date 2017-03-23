package org.stagemonitor.vertx.utils;

import io.vertx.core.eventbus.Message;
public class DefaultRequestNamer implements RequestNamer {
	@Override
	public String getRequestName(Message<?> msg) {
		return msg.address();
	}
}
