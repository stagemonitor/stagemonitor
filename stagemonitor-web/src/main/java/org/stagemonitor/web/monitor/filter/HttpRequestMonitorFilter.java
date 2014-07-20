package org.stagemonitor.web.monitor.filter;

import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.QueryParameterConfigurationSource;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class HttpRequestMonitorFilter extends AbstractExclusionFilter implements Filter {

	protected final Configuration configuration;
	protected final RequestMonitor requestMonitor;
	private final QueryParameterConfigurationSource queryParameterConfigurationSource;

	public HttpRequestMonitorFilter() {
		this(StageMonitor.getConfiguration());
	}

	public HttpRequestMonitorFilter(Configuration configuration) {
		this.configuration = configuration;
		queryParameterConfigurationSource = new QueryParameterConfigurationSource(configuration);
		requestMonitor = new RequestMonitor(configuration);
	}

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		MeasurementSession measurementSession = new MeasurementSession(getApplicationName(filterConfig),
				RequestMonitor.getHostName(), configuration.getInstanceName());
		requestMonitor.setMeasurementSession(measurementSession);
		configuration.addConfigurationSource(queryParameterConfigurationSource, true);
	}

	private String getApplicationName(FilterConfig filterConfig) {
		String name = configuration.getApplicationName();
		if (name == null || name.isEmpty()) {
			name = filterConfig.getServletContext().getServletContextName();
		}
		return name;
	}

	@Override
	public void doFilterInternal(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
			throws IOException, ServletException {
		if (configuration.isStagemonitorActive() && request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			final StatusExposingByteCountingServletResponse responseWrapper = new StatusExposingByteCountingServletResponse((HttpServletResponse) response);
			updateConfiguration(httpServletRequest);
			try {
				requestMonitor.monitor(new MonitoredHttpRequest(httpServletRequest,
						responseWrapper, filterChain, configuration));
			} catch (Exception e) {
				handleException(e);
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}

	private void updateConfiguration(HttpServletRequest httpServletRequest) {
		@SuppressWarnings("unchecked")
		final Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();

		final String configurationUpdatePassword = getFirstOrEmpty(parameterMap.get("stagemonitor.configuration.update.password"));
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			if ("stagemonitorReloadConfig".equals(entry.getKey())) {
				configuration.reload();
			} else if (entry.getKey().startsWith("stagemonitor.")) {
				queryParameterConfigurationSource.updateConfiguration(entry.getKey(), getFirstOrEmpty(entry.getValue()), configurationUpdatePassword);
			}
		}
	}

	private String getFirstOrEmpty(String[] strings) {
		if (strings != null && strings.length > 0) {
			return strings[0];
		}
		return "";
	}

	protected void handleException(Exception e) throws IOException, ServletException  {
		if (e instanceof IOException) {
			throw (IOException) e;
		}
		if (e instanceof ServletException) {
			throw (ServletException) e;
		}
		if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else {
			throw new RuntimeException(e);
		}
	}
}
