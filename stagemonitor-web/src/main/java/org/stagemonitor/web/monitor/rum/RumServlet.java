package org.stagemonitor.web.monitor.rum;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.web.WebPlugin;

/**
 * The Real User Monitoring Servlet handles boomerang beacons (see http://www.lognormal.com/boomerang/doc/)
 */
public class RumServlet extends HttpServlet {

	private final Metric2Registry metricRegistry;
	private final WebPlugin webPlugin;

	public RumServlet() {
		this(Stagemonitor.getMetric2Registry(), Stagemonitor.getConfiguration(WebPlugin.class));
	}

	public RumServlet(Metric2Registry metricRegistry, WebPlugin webPlugin) {
		this.metricRegistry = metricRegistry;
		this.webPlugin = webPlugin;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (webPlugin.isRealUserMonitoringEnabled()) {
			// Boomerang is requesting an image, so set appropriate header to not confuse browsers
			resp.setContentType("image/png");

			final String requestName = getRequiredParam(req, "requestName");
			final long serverTime = Long.parseLong(getRequiredParam(req, "serverTime"));
			final long timeToFirstByte = Long.parseLong(getRequiredParam(req, "timeToFirstByte"));
			final long domProcessing = Long.parseLong(getRequiredParam(req, "domProcessing"));
			final long pageRendering = Long.parseLong(getRequiredParam(req, "pageRendering"));
			final long networkTime = timeToFirstByte - serverTime;

			trackPageLoadTime("All", serverTime, domProcessing, pageRendering, networkTime);
			if (webPlugin.isCollectPageLoadTimesPerRequest()) {
				trackPageLoadTime(requestName, serverTime, domProcessing, pageRendering, networkTime);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void trackPageLoadTime(String requestName, long serverTime, long domProcessing, long pageRendering, long networkTime) {
		metricRegistry.timer(name("response_time_rum").tag("request_name", requestName).layer("Dom Processing").build()).update(domProcessing, MILLISECONDS);
		metricRegistry.timer(name("response_time_rum").tag("request_name", requestName).layer("Page Rendering").build()).update(pageRendering, MILLISECONDS);
		metricRegistry.timer(name("response_time_rum").tag("request_name", requestName).layer("Network").build()).update(networkTime, MILLISECONDS);
		metricRegistry.timer(name("response_time_rum").tag("request_name", requestName).layer("Server").build()).update(serverTime, MILLISECONDS);
		metricRegistry.timer(name("response_time_rum").tag("request_name", requestName).layer("All").build()).update(serverTime + networkTime + pageRendering + domProcessing, MILLISECONDS);
	}

	private String getRequiredParam(HttpServletRequest req, String parameterName) {
		final String parameter = req.getParameter(parameterName);
		if (parameter == null) {
			throw new IllegalArgumentException("Parameter " + parameterName + " missing");
		}
		return parameter;
	}
}
