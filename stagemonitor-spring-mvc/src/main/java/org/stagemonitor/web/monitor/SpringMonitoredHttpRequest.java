package org.stagemonitor.web.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.springmvc.SpringMvcPlugin;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SpringMonitoredHttpRequest extends MonitoredHttpRequest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final SpringMvcPlugin mvcPlugin;

	public SpringMonitoredHttpRequest(HttpServletRequest httpServletRequest,
									  StatusExposingByteCountingServletResponse statusExposingResponse,
									  FilterChain filterChain, Configuration configuration) {

		super(httpServletRequest, statusExposingResponse, filterChain, configuration);
		mvcPlugin = configuration.getConfig(SpringMvcPlugin.class);
	}

	@Override
	public String getRequestName() {
		String name = "";
		for (HandlerMapping handlerMapping : HandlerMappingServletContextListener.allHandlerMappings) {
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
		if (!mvcPlugin.isMonitorOnlySpringMvcRequests() || HandlerMappingServletContextListener.allHandlerMappings.isEmpty()) {
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
	public static class HandlerMappingServletContextListener implements ServletContextListener {

		private static List<HandlerMapping> allHandlerMappings = Collections.emptyList();

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			setAllHandlerMappings(getAllHandlerMappings(sce.getServletContext()));
		}

		static void setAllHandlerMappings(List<HandlerMapping> allHandlerMappings) {
			HandlerMappingServletContextListener.allHandlerMappings = allHandlerMappings;
		}

		private List<HandlerMapping> getAllHandlerMappings(ServletContext servletContext) {
			List<HandlerMapping> result = new ArrayList<HandlerMapping>();
			final Enumeration attributeNames = servletContext.getAttributeNames();
			while (attributeNames.hasMoreElements()) {
				String attributeName = (String) attributeNames.nextElement();
				if (attributeName.startsWith(FrameworkServlet.SERVLET_CONTEXT_PREFIX)) {
					result.add(WebApplicationContextUtils.getWebApplicationContext(
							servletContext, attributeName).getBean(RequestMappingHandlerMapping.class));
				}
			}
			return result;
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
		}
	}
}
