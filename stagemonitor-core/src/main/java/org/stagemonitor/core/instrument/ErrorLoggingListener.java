package org.stagemonitor.core.instrument;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ErrorLoggingListener extends AgentBuilder.Listener.Adapter {

	private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingListener.class);

	@Override
	public void onError(String typeName, ClassLoader classLoader, JavaModule javaModule, Throwable throwable) {
		logger.warn("ERROR on transformation " + typeName, throwable);
	}
}
