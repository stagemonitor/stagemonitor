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
			// Boomerang is requesting a image, so set appropriate header to not confuse browsers
			resp.setContentType("image/png");

			final String requestName = getRequiredParam(req, "requestName");
			final long serverTime = Long.parseLong(getRequiredParam(req, "serverTime"));
			final long timeToFirstByte = Long.parseLong(getRequiredParam(req, "timeToFirstByte"));
			final long domProcessing = Long.parseLong(getRequiredParam(req, "domProcessing"));
			final long pageRendering = Long.parseLong(getRequiredParam(req, "pageRendering"));
			final long totalTime = timeToFirstByte + domProcessing + pageRendering;
			final long networkTime = timeToFirstByte - serverTime;

			metricRegistry.timer(name("response_time").tag("request_name", "All").tag("tier", "browser").tag("layer", "dom_processing").build()).update(domProcessing, MILLISECONDS);
			metricRegistry.timer(name("response_time").tag("request_name", "All").tag("tier", "browser").tag("layer", "page_rendering").build()).update(pageRendering, MILLISECONDS);
			metricRegistry.timer(name("response_time").tag("request_name", "All").tag("tier", "network").tag("layer", "total").build()).update(networkTime, MILLISECONDS);
			metricRegistry.timer(name("response_time").tag("request_name", "All").tag("tier", "total").tag("layer", "total").build()).update(totalTime, MILLISECONDS);
			metricRegistry.timer(name("response_time").tag("request_name", "All").tag("tier", "server_rum").tag("layer", "total").build()).update(serverTime, MILLISECONDS);
			if (webPlugin.isCollectPageLoadTimesPerRequest()) {
				metricRegistry.timer(name("response_time").tag("request_name", requestName).tag("tier", "browser").tag("layer", "dom_processing").build()).update(domProcessing, MILLISECONDS);
				metricRegistry.timer(name("response_time").tag("request_name", requestName).tag("tier", "browser").tag("layer", "page_rendering").build()).update(pageRendering, MILLISECONDS);
				metricRegistry.timer(name("response_time").tag("request_name", requestName).tag("tier", "network").tag("layer", "total").build()).update(networkTime, MILLISECONDS);
				metricRegistry.timer(name("response_time").tag("request_name", requestName).tag("tier", "total").tag("layer", "total").build()).update(totalTime, MILLISECONDS);
				metricRegistry.timer(name("response_time").tag("request_name", requestName).tag("tier", "server_rum").tag("layer", "total").build()).update(serverTime, MILLISECONDS);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private String getRequiredParam(HttpServletRequest req, String parameterName) {
		final String parameter = req.getParameter(parameterName);
		if (parameter == null) {
			throw new IllegalArgumentException("Parameter " + parameterName + " missing");
		}
		return parameter;
	}
}
