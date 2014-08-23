package org.stagemonitor.web.monitor.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.SpringMonitoredHttpRequest;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SpringHttpRequestMonitorFilter extends HttpRequestMonitorFilter {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private List<HandlerMapping> allHandlerMappings;
	private ServletContext servletContext;

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		super.initInternal(filterConfig);
		try {
			servletContext = filterConfig.getServletContext();
		} catch (RuntimeException e) {
			logger.warn(e.getMessage() + " (this exception is ignored)", e);
		}
	}

	@Override
	protected void beforeFilter() {
		allHandlerMappings = ensureHandlerMappingsAreInitialized();
	}

	@Override
	protected RequestMonitor.RequestInformation<HttpRequestTrace> monitorRequest(FilterChain filterChain, HttpServletRequest httpServletRequest, StatusExposingByteCountingServletResponse responseWrapper) throws Exception {
		final SpringMonitoredHttpRequest monitoredRequest = new SpringMonitoredHttpRequest(httpServletRequest,
				responseWrapper, filterChain, configuration, allHandlerMappings);
		return requestMonitor.monitor(monitoredRequest);
	}

	private List<HandlerMapping> ensureHandlerMappingsAreInitialized() {
		List<HandlerMapping> result = allHandlerMappings;
		if (result == null) {
			synchronized (this) {
				result = allHandlerMappings;
				if (result == null) {
					try {
						allHandlerMappings = result = getAllHandlerMappings();
					} catch (RuntimeException e) {
						logger.warn(e.getMessage() + " (this exception is ignored)", e);
						allHandlerMappings = result = Collections.emptyList();
					}
				}
			}
		}
		return result;
	}

	private List<HandlerMapping> getAllHandlerMappings() {
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

}
