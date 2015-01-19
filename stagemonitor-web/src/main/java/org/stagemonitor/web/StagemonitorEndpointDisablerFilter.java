package org.stagemonitor.web;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Disables all endpoints under /stagemonitor/* if 'stagemonitor.web.widget.enabled' is set to 'false' unless the
 * request parameter or header 'stagemonitor.password' is provided with the correct password as value or
 * stagemonitor.password is set to a empty string.
 */
@WebFilter("/stagemonitor/*")
public class StagemonitorEndpointDisablerFilter implements Filter {

	private final WebPlugin webPlugin;
	private final Configuration configuration;

	public StagemonitorEndpointDisablerFilter() {
		this(Stagemonitor.getConfiguration(WebPlugin.class), Stagemonitor.getConfiguration());
	}

	public StagemonitorEndpointDisablerFilter(WebPlugin webPlugin, Configuration configuration) {
		this.webPlugin = webPlugin;
		this.configuration = configuration;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse &&
				!webPlugin.isWidgetAndStagemonitorEndpointsAllowed((HttpServletRequest) request)) {
			// let's pretent as if stagemonitor is not there to not leak information
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}
}
