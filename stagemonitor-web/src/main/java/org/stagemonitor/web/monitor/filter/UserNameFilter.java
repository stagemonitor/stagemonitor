package org.stagemonitor.web.monitor.filter;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.web.WebPlugin;

/**
 * Populates user name on the request trace. Dedicated to its own filter
 * so it can be configured to run as the last filter in the filter chain
 * as this information may not be available until that point.
 *
 */
public class UserNameFilter extends AbstractExclusionFilter {
	
	public UserNameFilter() {
		this(Stagemonitor.getConfiguration());
	}

	public UserNameFilter(Configuration configuration) {
		super(configuration.getConfig(WebPlugin.class).getExcludedRequestPaths());
	}

	@Override
	public void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {
		RequestTrace requestTrace = RequestMonitor.getRequest();
		if (requestTrace != null && requestTrace.getUsername() == null) {
			requestTrace.setAndAnonymizeUserName(getUserName(servletRequest));
		}
		
		filterChain.doFilter(servletRequest, servletResponse);
	}
	
	private static String getUserName(HttpServletRequest servletRequest) {
		Principal userPrincipal = servletRequest.getUserPrincipal();
		return userPrincipal != null ? userPrincipal.getName() : null;
	}
}
