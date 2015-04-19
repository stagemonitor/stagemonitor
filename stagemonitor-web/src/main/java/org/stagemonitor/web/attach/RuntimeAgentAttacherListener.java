package org.stagemonitor.web.attach;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.MainStagemonitorClassFileTransformer;

/**
 * Attaches the stagemonitor agent at runtime
 */
@WebListener
public class RuntimeAgentAttacherListener implements ServletContextListener {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public RuntimeAgentAttacherListener() {
		final CorePlugin configuration = Stagemonitor.getConfiguration(CorePlugin.class);
		if (configuration.isStagemonitorActive() && configuration.isAttachAgentAtRuntime()) {
			try {
				MainStagemonitorClassFileTransformer.performRuntimeAttachment();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}
