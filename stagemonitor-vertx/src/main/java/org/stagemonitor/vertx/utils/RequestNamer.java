package org.stagemonitor.vertx.utils;

import io.vertx.core.eventbus.Message;

public interface RequestNamer {
	String getRequestName(Message<?> msg);
}
