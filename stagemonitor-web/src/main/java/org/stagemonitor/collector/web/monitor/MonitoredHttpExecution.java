package org.stagemonitor.collector.web.monitor;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorApplicationContext;
import org.stagemonitor.collector.core.monitor.MonitoredExecution;
import org.stagemonitor.collector.web.monitor.filter.StatusExposingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

public class MonitoredHttpExecution implements MonitoredExecution<HttpExecutionContext> {

	protected final HttpServletRequest httpServletRequest;
	protected final FilterChain filterChain;
	protected final StatusExposingServletResponse statusExposingResponse;
	protected final Configuration configuration;
	private final MetricRegistry metricRegistry;

	public MonitoredHttpExecution(HttpServletRequest httpServletRequest,
								  StatusExposingServletResponse statusExposingResponse,
								  FilterChain filterChain, Configuration configuration) {
		this.httpServletRequest = httpServletRequest;
		this.filterChain = filterChain;
		this.statusExposingResponse = statusExposingResponse;
		this.configuration = configuration;
		metricRegistry = StageMonitorApplicationContext.getMetricRegistry();
	}

	@Override
	public String getInstanceName() {
		return httpServletRequest.getServerName();
	}

	@Override
	public HttpExecutionContext createExecutionContext() {
		HttpExecutionContext requestStats = new HttpExecutionContext();
		requestStats.setName(getRequestName());
		requestStats.setMethod(httpServletRequest.getMethod());
		requestStats.setUrl(httpServletRequest.getRequestURI());
		@SuppressWarnings("unchecked") // according to javadoc, its always a Map<String, String[]>
		final Map<String, String[]> parameterMap = (Map<String, String[]>) httpServletRequest.getParameterMap();
		requestStats.setParameter(getSafeQueryString(parameterMap));
		if (configuration.isCollectHeaders()) {
			requestStats.setHeaders(getHeaders(httpServletRequest));
		}
		return requestStats;
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

				queryStringBuilder.append(entry.getKey()).append('=');
				if (paramExcluded) {
					queryStringBuilder.append("XXXX");
				} else {
					queryStringBuilder.append(value);
				}
			}
		}
		return queryStringBuilder.toString();
	}

	private boolean isParamExcluded(String queryParameter) {
		final List<Pattern> confidentialQueryParams = configuration.getConfidentialQueryParams();
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
			final String headerName = (String) headerNames.nextElement();
			if (!excludedHeaders.contains(headerName.toLowerCase())) {
				headers.put(headerName, request.getHeader(headerName));
			}
		}
		return headers;
	}

	@Override
	public Object execute() throws Exception {
		filterChain.doFilter(httpServletRequest, statusExposingResponse);
		return null;
	}

	@Override
	public void onPostExecute(HttpExecutionContext executionContext) {
		int status = statusExposingResponse.getStatus();
		executionContext.setStatusCode(status);
		metricRegistry.counter(name("request.statuscode", Integer.toString(status))).inc();
		if (status >= 400) {
			executionContext.setError(true);
		}
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
