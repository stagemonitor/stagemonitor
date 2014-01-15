package de.isys.jawap.collector.web.monitor.filter;

import de.isys.jawap.collector.core.JawapApplicationContext;
import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.core.monitor.ExecutionContextMonitor;
import de.isys.jawap.collector.web.monitor.MonitoredHttpExecutionContext;
import de.isys.jawap.collector.web.rest.HttpExecutionContextRestClient;
import de.isys.jawap.entities.MeasurementSession;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpExecutionContextMonitorFiler extends AbstractExclusionFilter implements Filter {

	protected HttpExecutionContextRestClient httpExecutionContextRestClient = new HttpExecutionContextRestClient();
	protected Configuration configuration = JawapApplicationContext.getConfiguration();
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
				executionContextMonitor.monitor(new MonitoredHttpExecutionContext(httpServletRequest,
						statusExposingResponse, filterChain, configuration, httpExecutionContextRestClient));
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
