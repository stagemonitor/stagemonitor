package de.isys.jawap.collector.web.monitor;

import com.codahale.metrics.MetricRegistry;
import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.core.monitor.MonitoredExecution;
import de.isys.jawap.collector.web.monitor.filter.StatusExposingServletResponse;
import de.isys.jawap.entities.web.HttpRequestContext;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

public class HttpRequestContextMonitoredExecution extends MonitoredExecution<HttpRequestContext> {

	private final HttpServletRequest httpServletRequest;
	private final FilterChain filterChain;
	private final StatusExposingServletResponse statusExposingResponse;
	private final Configuration configuration;


	public HttpRequestContextMonitoredExecution(HttpServletRequest httpServletRequest,
												StatusExposingServletResponse statusExposingResponse,
												FilterChain filterChain, MetricRegistry metricRegistry,
												Configuration configuration) {
		super(metricRegistry);
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
		return requestURI;
	}

	@Override
	public String getTimerName(String requestName) {
		return name("web.request", httpServletRequest.getMethod(), requestName.replace('.', ':'));
	}

	@Override
	public HttpRequestContext getExecutionContext() {
		HttpRequestContext requestStats = new HttpRequestContext();
		requestStats.setUrl(httpServletRequest.getRequestURI());
		requestStats.setQueryParams(httpServletRequest.getQueryString());
		return requestStats;
	}

	@Override
	public void execute() throws Exception {
		filterChain.doFilter(httpServletRequest, statusExposingResponse);
	}

	@Override
	public void onPostExecute(HttpRequestContext executionContext) {
		int status = statusExposingResponse.getStatus();
		executionContext.setStatusCode(status);
		metricRegistry.counter(name("web.statuscode", Integer.toString(status))).inc();
		if (status >= 400) {
			executionContext.setFailed(true);
		}
	}

}
