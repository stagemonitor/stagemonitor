package org.stagemonitor.web.servlet.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class ServletContainerInitializerUtil {
	private static final Logger logger = LoggerFactory.getLogger(ServletContainerInitializerUtil.class);

	public static void registerStagemonitorServletContainerInitializers(ServletContext servletContext) {
		for (StagemonitorServletContainerInitializer sci : getStagemonitorSCIs()) {
			try {
				sci.onStartup(servletContext);
			} catch (ServletException e) {
				logger.warn("Ignored exception:", e);
			}
		}
	}

	public static List<StagemonitorServletContainerInitializer> getStagemonitorSCIs() {
		List<StagemonitorServletContainerInitializer> sciPlugins = new ArrayList<StagemonitorServletContainerInitializer>();
		for (StagemonitorServletContainerInitializer plugin : ServiceLoader
				.load(StagemonitorServletContainerInitializer.class, ServletContainerInitializerUtil.class.getClassLoader())) {
			sciPlugins.add(plugin);
		}
		return sciPlugins;
	}

	public static boolean avoidDoubleInit(StagemonitorServletContainerInitializer sci, ServletContext ctx) {
		final String initializedAttribute = sci.getClass().getName() + ".initialized";
		if (ctx.getAttribute(initializedAttribute) != null) {
			// already initialized
			return true;
		}
		ctx.setAttribute(initializedAttribute, true);
		return false;
	}
}
