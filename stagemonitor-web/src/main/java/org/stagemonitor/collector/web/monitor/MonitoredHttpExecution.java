package org.stagemonitor.collector.web.monitor;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitor;
import org.stagemonitor.collector.monitor.MonitoredExecution;
import org.stagemonitor.collector.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

public class MonitoredHttpExecution implements MonitoredExecution<HttpExecutionContext> {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected final HttpServletRequest httpServletRequest;
	protected final FilterChain filterChain;
	protected final StatusExposingByteCountingServletResponse responseWrapper;
	protected final Configuration configuration;
	private final MetricRegistry metricRegistry;

	public MonitoredHttpExecution(HttpServletRequest httpServletRequest,
								  StatusExposingByteCountingServletResponse responseWrapper,
								  FilterChain filterChain, Configuration configuration) {
		this.httpServletRequest = httpServletRequest;
		this.filterChain = filterChain;
		this.responseWrapper = responseWrapper;
		this.configuration = configuration;
		metricRegistry = StageMonitor.getMetricRegistry();
	}

	@Override
	public String getInstanceName() {
		return httpServletRequest.getServerName();
	}

	@Override
	public HttpExecutionContext createExecutionContext() {
		HttpExecutionContext executionContext = new HttpExecutionContext();
		executionContext.setName(getRequestName());
		executionContext.setMethod(httpServletRequest.getMethod());
		executionContext.setUrl(httpServletRequest.getRequestURI());
		executionContext.setClientIp(getClientIp(httpServletRequest));
		final Principal userPrincipal = httpServletRequest.getUserPrincipal();
		executionContext.setUsername(userPrincipal != null ? userPrincipal.getName() : null);
		@SuppressWarnings("unchecked") // according to javadoc, its always a Map<String, String[]>
		final Map<String, String[]> parameterMap = (Map<String, String[]>) httpServletRequest.getParameterMap();
		executionContext.setParameter(getSafeQueryString(parameterMap));
		if (configuration.isCollectHeaders()) {
			executionContext.setHeaders(getHeaders(httpServletRequest));
		}
		return executionContext;
	}

	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	public String getRequestName() {
		String requestURI = httpServletRequest.getRequestURI();
		for (Map.Entry<Pattern, String> entry : configuration.getGroupUrls().entrySet()) {
			requestURI = entry.getKey().matcher(requestURI).replaceAll(entry.getValue());
		}
		return httpServletRequest.getMethod() + " " +requestURI;
	}

	private String getSafeQueryString(Map<String, String[]> parameterMap) {
		StringBuilder queryStringBuilder = new StringBuilder();
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			final boolean paramExcluded = isParamExcluded(entry.getKey());
			for (String value : entry.getValue()) {
				if (queryStringBuilder.length() == 0) {
					queryStringBuilder.append('?');
				} else {
					queryStringBuilder.append('&');
				}

				queryStringBuilder.append(entry.getKey());
				if (paramExcluded) {
					queryStringBuilder.append('=').append("XXXX");
				} else {
					queryStringBuilder.append('=').append(value);
				}
			}
		}
		return queryStringBuilder.toString();
	}

	private boolean isParamExcluded(String queryParameter) {
		final List<Pattern> confidentialQueryParams = configuration.getConfidentialRequestParams();
		for (Pattern excludedParam : confidentialQueryParams) {
			if (excludedParam.matcher(queryParameter).matches()) {
				return true;
			}
		}
		return false;
	}

	private Map<String, String> getHeaders(HttpServletRequest request) {
		Map<String, String> headers = new HashMap<String, String>();
		final Enumeration headerNames = request.getHeaderNames();
		final List<String> excludedHeaders = configuration.getExcludedHeaders();
		while (headerNames.hasMoreElements()) {
			final String headerName = ((String) headerNames.nextElement()).toLowerCase();
			if (!excludedHeaders.contains(headerName)) {
				headers.put(headerName, request.getHeader(headerName));
			}
		}
		return headers;
	}

	@Override
	public Object execute() throws Exception {
		filterChain.doFilter(httpServletRequest, responseWrapper);
		return null;
	}

	@Override
	public void onPostExecute(HttpExecutionContext executionContext) {
		int status = responseWrapper.getStatus();
		executionContext.setStatusCode(status);
		metricRegistry.counter(name("request.statuscode", Integer.toString(status))).inc();
		if (status >= 400) {
			executionContext.setError(true);
		}

		Object exception = httpServletRequest.getAttribute("exception");
		if (exception instanceof Exception) {
			executionContext.setException((Exception) exception);
		}
		executionContext.setBytesWritten(responseWrapper.getContentLength());
	}

	/**
	 * In a web context, we only want to monitor forwarded requests.
	 * If a request to /a makes a
	 * {@link javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 * to /b, we only want to collect metrics for /b, because it is the request, that does the actual computation.
	 *
	 * @return true
	 */
	@Override
	public boolean isMonitorForwardedExecutions() {
		return true;
	}
}
