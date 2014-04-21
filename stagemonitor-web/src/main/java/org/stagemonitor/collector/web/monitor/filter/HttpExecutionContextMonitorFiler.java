package org.stagemonitor.collector.web.monitor.filter;

import org.stagemonitor.collector.core.Configuration;
import org.stagemonitor.collector.core.StageMonitorApplicationContext;
import org.stagemonitor.collector.core.monitor.ExecutionContextMonitor;
import org.stagemonitor.collector.web.monitor.MonitoredHttpExecution;
import org.stagemonitor.collector.core.MeasurementSession;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpExecutionContextMonitorFiler extends AbstractExclusionFilter implements Filter {

	protected Configuration configuration = StageMonitorApplicationContext.getConfiguration();
	protected ExecutionContextMonitor executionContextMonitor = new ExecutionContextMonitor(configuration);

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		MeasurementSession measurementSession = new MeasurementSession();
		measurementSession.setApplicationName(getApplicationName(filterConfig));
		measurementSession.setHostName(ExecutionContextMonitor.getHostName());
		measurementSession.setInstanceName(configuration.getInstanceName());
		executionContextMonitor.setMeasurementSession(measurementSession);
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
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			final StatusExposingServletResponse statusExposingResponse = new StatusExposingServletResponse((HttpServletResponse) response);
			try {
				executionContextMonitor.monitor(new MonitoredHttpExecution(httpServletRequest,
						statusExposingResponse, filterChain, configuration));
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
		} if (e instanceof ServletException) {
			throw (ServletException) e;
		} if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void destroy() {
		executionContextMonitor.onPreDestroy();
	}

}
