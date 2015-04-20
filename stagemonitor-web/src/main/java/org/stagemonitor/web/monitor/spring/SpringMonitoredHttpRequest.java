package org.stagemonitor.web.monitor.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.stagemonitor.core.util.ClassUtils;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

public class SpringMonitoredHttpRequest extends MonitoredHttpRequest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public SpringMonitoredHttpRequest(HttpServletRequest httpServletRequest,
									  StatusExposingByteCountingServletResponse statusExposingResponse,
									  FilterChain filterChain, Configuration configuration) {

		super(httpServletRequest, statusExposingResponse, filterChain, configuration);
	}

	@Override
	public String getRequestName() {
		String name = "";
		for (HandlerMapping handlerMapping : (List<HandlerMapping>) HandlerMappingServletContextListener.allHandlerMappings) {
			try {
				HandlerExecutionChain handler = handlerMapping.getHandler(httpServletRequest);
				name = getRequestNameFromHandler(handler);
			} catch (Exception e) {
				// ignore, try next
				logger.warn(e.getMessage(), e);
			}

			if (!name.isEmpty()) {
				return name;
			}
		}
		if (!webPlugin.isMonitorOnlySpringMvcRequests() || HandlerMappingServletContextListener.allHandlerMappings.isEmpty()) {
			name = super.getRequestName();
		}
		return name;
	}

	public static String getRequestNameFromHandler(HandlerExecutionChain handler) {
		if (handler != null && handler.getHandler() instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
			return StringUtils.splitCamelCase(StringUtils.capitalize(handlerMethod.getMethod().getName()));
		}
		return "";
	}

	@WebListener
	public static class HandlerMappingServletContextListener implements ServletContextListener, ServletRequestListener {

		// don't use typed list so that wildfly does not fail if HandlerMapping is not on the classpath
		private static List/*<HandlerMapping>*/ allHandlerMappings = Collections.emptyList();
		private ServletContext servletContext;
		private boolean springMvc;

		public HandlerMappingServletContextListener() {
			springMvc = ClassUtils.isPresent("org.springframework.web.servlet.HandlerMapping");
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			if (springMvc) {
				servletContext = sce.getServletContext();
			}
		}

		@Override
		public void requestInitialized(ServletRequestEvent sre) {
			if (springMvc && allHandlerMappings.isEmpty()) {
				HandlerMappingsExtractor.initAllHandlerMappings(servletContext);
			}
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
		}

		@Override
		public void requestDestroyed(ServletRequestEvent sre) {
		}

	}

	static class HandlerMappingsExtractor {

		private synchronized static void initAllHandlerMappings(ServletContext servletContext) {
			if (HandlerMappingServletContextListener.allHandlerMappings.isEmpty()) {
				HandlerMappingServletContextListener.allHandlerMappings = getAllHandlerMappings(servletContext);
			}
		}

		static void setAllHandlerMappings(List<HandlerMapping> handlerMappings) {
			HandlerMappingServletContextListener.allHandlerMappings = handlerMappings;
		}

		private static List<HandlerMapping> getAllHandlerMappings(ServletContext servletContext) {
			List<HandlerMapping> result = new ArrayList<HandlerMapping>();
			final Enumeration attributeNames = servletContext.getAttributeNames();
			while (attributeNames.hasMoreElements()) {
				String attributeName = (String) attributeNames.nextElement();
				if (attributeName.startsWith(FrameworkServlet.SERVLET_CONTEXT_PREFIX)) {
					ApplicationContext applicationContext = WebApplicationContextUtils
							.getWebApplicationContext(servletContext, attributeName);
					do {
						result.addAll(applicationContext.getBeansOfType(RequestMappingHandlerMapping.class).values());
						applicationContext = applicationContext.getParent();
					} while (applicationContext != null);
				}
			}
			return result;
		}

	}
}
