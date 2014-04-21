package org.stagemonitor.collector.web.monitor.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Adds support for excluding specific url prefixes of a filter-mapping.
 * <p/>
 * Example:
 * <pre>
 *   &lt;filter>
 *      &lt;filter-name>theFilter&lt;/filter-name>
 *      &lt;filter-class>some filter exdending {@link AbstractExclusionFilter}&lt;/filter-class>
 *      &lt;init-param>
 *           &lt;param-name>exclude&lt;/param-name>
 *           &lt;param-value>rest, /picture/&lt;/param-value>
 *      &lt;/init-param>
 *   &lt;/filter>
 * </pre>
 * <p/>
 * In this example, the filter won't be executed for all URIs starting with /rest or /picture
 * <p/>
 * In other words, the following will be checked for each exclude prefix: {@code httpServletRequest.getRequestURI().startsWith(excludedPath)}
 *
 * @author fbarnsteiner
 */
public abstract class AbstractExclusionFilter implements Filter {

	private List<String> excludedPaths;


	@Override
	public final void init(FilterConfig filterConfig) throws ServletException {
		String excludeParam = filterConfig.getInitParameter("exclude");
		if (excludeParam != null && !excludeParam.isEmpty()) {
			setExcludedPaths(Arrays.asList(excludeParam.split(",")));
		}
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
	public final void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
							   FilterChain filterChain) throws IOException, ServletException {
		if (servletRequest instanceof HttpServletRequest) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

			if (isExcluded(httpServletRequest)) {
				// Don't execute Filter when request matches exclusion pattern
				filterChain.doFilter(servletRequest, servletResponse);
			} else {
				doFilterInternal(servletRequest, servletResponse, filterChain);
			}
		} else {
			doFilterInternal(servletRequest, servletResponse, filterChain);
		}
	}

	private boolean isExcluded(HttpServletRequest httpServletRequest) {
		if (excludedPaths == null) {
			return false;
		}
		for (String excludedPath : excludedPaths) {
			if (httpServletRequest.getRequestURI().startsWith(excludedPath)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void destroy() {
	}

	public abstract void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse,
										  FilterChain filterChain) throws IOException, ServletException;


	private void setExcludedPaths(List<String> excludedPaths) {
		this.excludedPaths = new ArrayList<String>(excludedPaths.size());
		for (String exclude : excludedPaths) {
			exclude = exclude.trim();
			if (exclude != null && !exclude.isEmpty()) {
				if (!exclude.startsWith("/")) {
					exclude = "/" + exclude;
				}
				this.excludedPaths.add(exclude);
			}
		}
	}


}
