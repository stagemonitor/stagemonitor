package org.stagemonitor.collector.web.monitor.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.stagemonitor.collector.web.monitor.SpringMonitoredHttpExecution;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SpringHttpExecutionContextMonitorFiler extends HttpExecutionContextMonitorFiler {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private List<HandlerMapping> allHandlerMappings;
	private ServletContext servletContext;

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		super.initInternal(filterConfig);
		try {
			servletContext = filterConfig.getServletContext();
		} catch (RuntimeException e) {
			logger.error(e.getMessage() + " (this exception is ignored)", e);
		}
	}

	@Override
	public void doFilterInternal(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
			throws IOException, ServletException {
		allHandlerMappings = ensureHandlerMappingsAreInitialized();

		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			final StatusExposingServletResponse statusExposingResponse = new StatusExposingServletResponse((HttpServletResponse) response);
			try {
				executionContextMonitor.monitor(new SpringMonitoredHttpExecution(httpServletRequest,
						statusExposingResponse, filterChain, configuration, allHandlerMappings));
			} catch (Exception e) {
				handleException(e);
			}
		} else {
			filterChain.doFilter(request, response);
		}
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
						logger.error(e.getMessage() + " (this exception is ignored)", e);
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
