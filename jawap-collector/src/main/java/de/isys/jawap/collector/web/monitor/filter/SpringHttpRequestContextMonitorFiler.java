package de.isys.jawap.collector.web.monitor.filter;

import de.isys.jawap.collector.web.monitor.SpringHttpRequestContextMonitoredExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SpringHttpRequestContextMonitorFiler extends HttpRequestContextMonitorFiler {
	private final Log logger = LogFactory.getLog(getClass());

	private List<RequestMappingHandlerMapping> allHandlerMappings;
	private ServletContext servletContext;

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		super.initInternal(filterConfig);
		try {
			servletContext = filterConfig.getServletContext();
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
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
				executionContextMonitor.monitor(new SpringHttpRequestContextMonitoredExecution(httpServletRequest,
						statusExposingResponse, filterChain, configuration, allHandlerMappings));
			} catch (Exception e) {
				handleException(e);
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}

	private List<RequestMappingHandlerMapping> ensureHandlerMappingsAreInitialized() {
		List<RequestMappingHandlerMapping> result = allHandlerMappings;
		if (result == null) {
			synchronized (this) {
				result = allHandlerMappings;
				if (result == null) {
					try {
						allHandlerMappings = result = getAllHandlerMappings();
					} catch (RuntimeException e) {
						logger.error(e.getMessage(), e);
						allHandlerMappings = result = Collections.emptyList();
					}
				}
			}
		}
		return result;
	}

	private List<RequestMappingHandlerMapping> getAllHandlerMappings() {
		List<RequestMappingHandlerMapping> result = new ArrayList<RequestMappingHandlerMapping>();
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
