package de.isys.jawap.collector.web.monitor;

import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.core.monitor.MonitoredExecution;
import de.isys.jawap.collector.web.monitor.filter.StatusExposingServletResponse;
import de.isys.jawap.entities.web.HttpExecutionContext;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

public class MonitoredHttpExecutionContext extends MonitoredExecution<HttpExecutionContext> {

	protected final HttpServletRequest httpServletRequest;
	protected final FilterChain filterChain;
	protected final StatusExposingServletResponse statusExposingResponse;
	protected final Configuration configuration;


	public MonitoredHttpExecutionContext(HttpServletRequest httpServletRequest,
												StatusExposingServletResponse statusExposingResponse,
												FilterChain filterChain, Configuration configuration) {
		this.httpServletRequest = httpServletRequest;
		this.filterChain = filterChain;
		this.statusExposingResponse = statusExposingResponse;
		this.configuration = configuration;
	}

	@Override
	public String getInstanceName() {
		return httpServletRequest.getServerName();
	}

	@Override
	public String getRequestName() {
		String requestURI = httpServletRequest.getRequestURI();
		for (Map.Entry<Pattern, String> entry : configuration.getGroupUrls().entrySet()) {
			requestURI = entry.getKey().matcher(requestURI).replaceAll(entry.getValue());
		}
		return httpServletRequest.getMethod() + " " +requestURI;
	}

	@Override
	public HttpExecutionContext getExecutionContext() {
		HttpExecutionContext requestStats = new HttpExecutionContext();
		requestStats.setMethod(httpServletRequest.getMethod());
		requestStats.setUrl(httpServletRequest.getRequestURI());
		requestStats.setQueryParams(getSafeQueryString(httpServletRequest.getParameterMap()));
		if (configuration.isCollectHeaders()) {
			requestStats.setHeader(getHeadersAsString(httpServletRequest));
		}
		return requestStats;
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

	private String getHeadersAsString(HttpServletRequest request) {
		StringBuilder headerStringBuilder = new StringBuilder();
		final Enumeration headerNames = request.getHeaderNames();
		final List<String> excludedHeaders = configuration.getExcludedHeaders();
		while (headerNames.hasMoreElements()) {
			final String headerName = (String) headerNames.nextElement();
			if (!excludedHeaders.contains(headerName.toLowerCase())) {
				headerStringBuilder.append(headerName).append(": ").append(request.getHeader(headerName)).append('\n');
			}
		}
		return headerStringBuilder.toString();
	}

	@Override
	public void execute() throws Exception {
		filterChain.doFilter(httpServletRequest, statusExposingResponse);
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

}
