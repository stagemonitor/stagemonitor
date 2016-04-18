package org.stagemonitor.core.instrument;

import net.bytebuddy.agent.builder.AgentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ErrorLoggingListener extends AgentBuilder.Listener.Adapter {

	private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingListener.class);

	@Override
	public void onError(String typeName, ClassLoader classLoader, Throwable throwable) {
		logger.warn("ERROR on transformation " + typeName, throwable);
	}
}
