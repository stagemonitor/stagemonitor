package de.isys.jawap;

import org.springframework.security.web.util.AntPathRequestMatcher;
import org.springframework.security.web.util.RequestMatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Adds support for excluding specific url patterns (ant-style patterns) of a filter-mapping.
 * <p/>
 * Example:
 * <pre>
 *   &lt;filter>
 *      &lt;filter-name>theFilter&lt;/filter-name>
 *      &lt;filter-class>org.springframework.web.filter.DelegatingFilterProxy&lt;/filter-class>
 *      &lt;init-param>
 *           &lt;param-name>exclude&lt;/param-name>
 *           &lt;param-value>rest/**, /picture/**&lt;/param-value>
 *      &lt;/init-param>
 *   &lt;/filter>
 * </pre>
 *
 * @author fbarnsteiner
 */
public abstract class AbstractExclusionFilter implements Filter {


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String excludeParam = filterConfig.getInitParameter("exclude");
		setExcludedPaths(Arrays.asList(excludeParam.split(",")));
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
		if (getExcludedPaths() == null) {
			return false;
		}
		for (RequestMatcher requestMatcher : getExcludedPaths()) {
			if (requestMatcher.matches(httpServletRequest)) {
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

	public abstract List<RequestMatcher> getExcludedPaths();

	public void setExcludedPaths(List<String> excludedPaths) {
		for (String exclude : excludedPaths) {
			exclude = exclude.trim();
			if (exclude != null && !exclude.isEmpty()) {
				if (!exclude.startsWith("/")) {
					exclude = "/" + exclude;
				}
				getExcludedPaths().add(new AntPathRequestMatcher(exclude));
			}
		}
	}


}
