package de.isys.jawap.collector.web.monitor.filter;

import de.isys.jawap.collector.web.monitor.SpringHttpRequestContextMonitoredExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SpringHttpRequestContextMonitorFiler extends HttpRequestContextMonitorFiler {
	private final Log logger = LogFactory.getLog(getClass());

	//	@Autowired
	private RequestMappingHandlerMapping handlerMapping;
	private ServletContext servletContext;
	private String contextAttribute;

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		super.initInternal(filterConfig);
		try {
			servletContext = filterConfig.getServletContext();
			contextAttribute = filterConfig.getInitParameter("contextAttribute");
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void doFilterInternal(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
			throws IOException, ServletException {
		handlerMapping = getHandlerMapping();

		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			final StatusExposingServletResponse statusExposingResponse = new StatusExposingServletResponse((HttpServletResponse) response);
			try {
				executionContextMonitor.monitor(new SpringHttpRequestContextMonitoredExecution(httpServletRequest,
						statusExposingResponse, filterChain, configuration, handlerMapping));
			} catch (Exception e) {
				handleException(e);
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}

	private RequestMappingHandlerMapping getHandlerMapping() {
		RequestMappingHandlerMapping result = handlerMapping;
		if (result == null) {
			synchronized (this) {
				result = handlerMapping;
				if (result == null) {
					try {
						handlerMapping = result = WebApplicationContextUtils.getWebApplicationContext(
								servletContext, contextAttribute).getBean(RequestMappingHandlerMapping.class);
					} catch (RuntimeException e) {
						logger.error(e.getMessage(), e);
						handlerMapping = result = new RequestMappingHandlerMapping();
					}
				}
			}
		}
		return result;
	}

}
