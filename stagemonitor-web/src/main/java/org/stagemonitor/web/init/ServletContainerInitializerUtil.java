package org.stagemonitor.web.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.configuration.ConfigurationOptionProvider;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class ServletContainerInitializerUtil {
	private static final Logger logger = LoggerFactory.getLogger(ServletContainerInitializerUtil.class);

	public static void registerStagemonitorServletContainerInitializers(ServletContext servletContext) {
		for (ServletContainerInitializer sci : getStagemonitorSCIs()) {
			try {
				sci.onStartup(null, servletContext);
			} catch (ServletException e) {
				logger.warn("Ignored exception:", e);
			}
		}
	}

	private static List<ServletContainerInitializer> getStagemonitorSCIs() {
		List<ServletContainerInitializer> sciPlugins = new ArrayList<ServletContainerInitializer>();
		for (ConfigurationOptionProvider plugin : Stagemonitor.getConfiguration().getConfigurationOptionProviders()) {
			if (plugin instanceof ServletContainerInitializer) {
				sciPlugins.add((ServletContainerInitializer) plugin);
			}
		}
		return sciPlugins;
	}

	public static boolean avoidDoubleInit(ServletContainerInitializer sci, ServletContext ctx) {
		final String initializedAttribute = sci.getClass().getName() + ".initialized";
		if (ctx.getAttribute(initializedAttribute) != null) {
			// already initialized
			return true;
		}
		ctx.setAttribute(initializedAttribute, true);
		return false;
	}
}
