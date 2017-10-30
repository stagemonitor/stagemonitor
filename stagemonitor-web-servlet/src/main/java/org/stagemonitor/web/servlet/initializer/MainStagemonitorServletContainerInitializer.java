package org.stagemonitor.web.servlet.initializer;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class MainStagemonitorServletContainerInitializer implements ServletContainerInitializer {
	@Override
	public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
		ServletContainerInitializerUtil.registerStagemonitorServletContainerInitializers(ctx);
	}
}
