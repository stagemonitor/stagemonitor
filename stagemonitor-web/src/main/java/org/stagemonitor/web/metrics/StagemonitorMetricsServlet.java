package org.stagemonitor.web.metrics;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.MetricNameFilter;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.Metric2RegistryModule;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.web.WebPlugin;

/**
 * A servlet which returns the metrics in a given registry as an {@code application/json} response.
 * Derived from com.codahale.metrics.servlets.MetricsServlet
 * (https://github.com/dropwizard/metrics/blob/master/metrics-servlets/src/main/java/com/codahale/metrics/servlets/MetricsServlet.java)
 */
public class StagemonitorMetricsServlet extends HttpServlet {

	private final transient Metric2Registry registry;
	private final transient WebPlugin webPlugin;
	private final transient ObjectMapper mapper;

	public StagemonitorMetricsServlet() {
		this(Stagemonitor.getMetric2Registry(), Stagemonitor.getPlugin(WebPlugin.class), JsonUtils.getMapper());
	}

	public StagemonitorMetricsServlet(Metric2Registry registry, WebPlugin webPlugin, ObjectMapper mapper) {
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
		ObjectMapper objectMapperCopy = mapper.copy();

		registerMetricsRegistryModule(request, objectMapperCopy);

		if (Boolean.parseBoolean(request.getParameter("pretty"))) {
			return objectMapperCopy.writerWithDefaultPrettyPrinter();
		}
		return objectMapperCopy.writer();
	}

	private void registerMetricsRegistryModule(HttpServletRequest request, ObjectMapper objectMapperCopy) {
		Metric2Filter metricFilter = Metric2Filter.ALL;
		final String[] metricNames = request.getParameterValues("metricNames[]");
		if (metricNames != null && metricNames.length > 0) {
			List<MetricName> metricNameList = new ArrayList<MetricName>(metricNames.length);
			for (String metricName : metricNames) {
				metricNameList.add(name(metricName).build());
			}
			metricFilter = new MetricNameFilter(metricNameList);
		}
		objectMapperCopy.registerModule(new Metric2RegistryModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, metricFilter));
	}
}
