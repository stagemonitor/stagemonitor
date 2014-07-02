package org.stagemonitor.web.monitor.filter;

import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpRequestMonitorFilter extends AbstractExclusionFilter implements Filter {

	protected Configuration configuration = StageMonitor.getConfiguration();
	protected RequestMonitor requestMonitor = new RequestMonitor(configuration);

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		MeasurementSession measurementSession = new MeasurementSession(getApplicationName(filterConfig),
				RequestMonitor.getHostName(), configuration.getInstanceName());
		requestMonitor.setMeasurementSession(measurementSession);
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
