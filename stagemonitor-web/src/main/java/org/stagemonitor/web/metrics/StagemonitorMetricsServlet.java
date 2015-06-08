package org.stagemonitor.web.metrics;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.web.WebPlugin;

/**
 * A servlet which returns the metrics in a given registry as an {@code application/json} response.
 * Derived from com.codahale.metrics.servlets.MetricsServlet
 * (https://github.com/dropwizard/metrics/blob/master/metrics-servlets/src/main/java/com/codahale/metrics/servlets/MetricsServlet.java)
 */
public class StagemonitorMetricsServlet extends HttpServlet {

	private final MetricRegistry registry;
	private final WebPlugin webPlugin;
	private final ObjectMapper mapper;

	public StagemonitorMetricsServlet() {
		this(Stagemonitor.getMetricRegistry(), Stagemonitor.getConfiguration(WebPlugin.class), JsonUtils.getMapper());
	}

	public StagemonitorMetricsServlet(MetricRegistry registry, WebPlugin webPlugin, ObjectMapper mapper) {
		this.registry = registry;
		this.webPlugin = webPlugin;
		this.mapper = mapper;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		if (webPlugin.getMetricsServletAllowedOrigin() != null) {
			resp.setHeader("Access-Control-Allow-Origin", webPlugin.getMetricsServletAllowedOrigin());
		}
		resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
		resp.setStatus(HttpServletResponse.SC_OK);

		final OutputStream output = resp.getOutputStream();
		try {
			String jsonpParamName = webPlugin.getMetricsServletJsonpParamName();
			if (jsonpParamName != null && req.getParameter(jsonpParamName) != null) {
				getWriter(req).writeValue(output, new JSONPObject(req.getParameter(jsonpParamName), registry));
			} else {
				getWriter(req).writeValue(output, registry);
			}
		} finally {
			output.close();
		}
	}

	private ObjectWriter getWriter(HttpServletRequest request) {
		if (Boolean.parseBoolean(request.getParameter("pretty"))) {
			return mapper.writerWithDefaultPrettyPrinter();
		}
		return mapper.writer();
	}
}
