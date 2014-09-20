package org.stagemonitor.web.monitor.rum;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.util.GraphiteSanitizer;
import org.stagemonitor.web.WebPlugin;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * The Real User Monitoring Servlet handles boomerang beacons (see http://www.lognormal.com/boomerang/doc/)
 */
public class RumServlet extends HttpServlet {

	private final MetricRegistry metricRegistry;
	private final WebPlugin webPlugin;

	public RumServlet(MetricRegistry metricRegistry, WebPlugin webPlugin) {
		this.metricRegistry = metricRegistry;
		this.webPlugin = webPlugin;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String timerName = name(GraphiteSanitizer.sanitizeGraphiteMetricSegment(getRequiredParam(req, "requestName")));
		final long serverTime = Long.parseLong(getRequiredParam(req, "serverTime"));
		final long timeToFirstByte = Long.parseLong(getRequiredParam(req, "timeToFirstByte"));
		final long domProcessing = Long.parseLong(getRequiredParam(req, "domProcessing"));
		final long pageRendering = Long.parseLong(getRequiredParam(req, "pageRendering"));
		final long totalTime = timeToFirstByte + domProcessing + pageRendering;
		final long networkTime = timeToFirstByte - serverTime;

		metricRegistry.timer("request.All.browser.time.dom-processing").update(domProcessing, MILLISECONDS);
		metricRegistry.timer("request.All.browser.time.page-rendering").update(pageRendering, MILLISECONDS);
		metricRegistry.timer("request.All.network.time.total").update(networkTime, MILLISECONDS);
		metricRegistry.timer("request.All.total.time.total").update(totalTime, MILLISECONDS);
		metricRegistry.timer("request.All.server-rum.time.total").update(serverTime, MILLISECONDS);
		if (webPlugin.isCollectPageLoadTimesPerRequest()) {
			metricRegistry.timer(name("request", timerName, "browser.time.dom-processing")).update(domProcessing, MILLISECONDS);
			metricRegistry.timer(name("request", timerName, "browser.time.page-rendering")).update(pageRendering, MILLISECONDS);
			metricRegistry.timer(name("request", timerName, "network.time.total")).update(networkTime, MILLISECONDS);
			metricRegistry.timer(name("request", timerName, "total.time.total")).update(totalTime, MILLISECONDS);
			metricRegistry.timer(name("request", timerName, "server-rum.time.total")).update(serverTime, MILLISECONDS);
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
