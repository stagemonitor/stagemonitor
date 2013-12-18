package de.isys.jawap.collector.web.monitor.filter;

import com.codahale.metrics.MetricRegistry;
import de.isys.jawap.collector.core.ApplicationContext;
import de.isys.jawap.collector.core.Configuration;
import de.isys.jawap.collector.core.monitor.ExecutionContextMonitor;
import de.isys.jawap.collector.web.monitor.HttpRequestContextMonitoredExecution;
import de.isys.jawap.entities.MeasurementSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;

// TODO as Aspect of Servlet+.service(..) or HttpServletRequest.new ?
public class HttpRequestContextMonitorFiler extends AbstractExclusionFilter implements Filter {

	private final Log logger = LogFactory.getLog(getClass());

	private final MetricRegistry metricRegistry = ApplicationContext.getMetricRegistry();
	private Configuration configuration = ApplicationContext.getConfiguration();
	private ExecutionContextMonitor executionContextMonitor = new ExecutionContextMonitor(metricRegistry, configuration);

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
				executionContextMonitor.monitor(new HttpRequestContextMonitoredExecution(httpServletRequest,
						statusExposingResponse, filterChain, metricRegistry, configuration));
			} catch (IOException e) {
				throw e;
			} catch (ServletException e) {
				throw e;
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		executionContextMonitor.onPreDestroy();
	}

}
