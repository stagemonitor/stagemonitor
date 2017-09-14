package org.stagemonitor.web.servlet.filter;

import org.stagemonitor.configuration.ConfigurationOption;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Adds support for excluding specific url prefixes of a filter-mapping.
 * <p>
 * The following will be checked for each exclude prefix: {@code httpServletRequest.getRequestURI().startsWith(excludedPath)}
 */
public abstract class AbstractExclusionFilter implements Filter {

	private ConfigurationOption<Collection<String>> excludedPaths;

	protected AbstractExclusionFilter(ConfigurationOption<Collection<String>> excludedPaths) {
		this.excludedPaths = excludedPaths;
	}

	@Override
	public final void init(FilterConfig filterConfig) throws ServletException {
		initInternal(filterConfig);
	}

	/**
	 * Can be used by subclasses to do their initialisation
	 *
	 * @see Filter#init(javax.servlet.FilterConfig)
	 */
	public void initInternal(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
							   FilterChain filterChain) throws IOException, ServletException {
		if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

			if (isExcluded(httpServletRequest)) {
				// Don't execute Filter when request matches exclusion pattern
				filterChain.doFilter(servletRequest, servletResponse);
			} else {
				doFilterInternal(httpServletRequest, (HttpServletResponse) servletResponse, filterChain);
			}
		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	private boolean isExcluded(HttpServletRequest request) {
		if (excludedPaths == null) {
			return false;
		}
		for (String excludedPath : excludedPaths.get()) {
			if (request.getRequestURI().substring(request.getContextPath().length()).startsWith(excludedPath)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void destroy() {
	}

	public abstract void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
										  FilterChain filterChain) throws IOException, ServletException;


}
