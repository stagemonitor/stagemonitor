package org.stagemonitor.web.alert;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.Check;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.configuration.ConfigurationServlet;

/**
 * A servlet that handles {@link Check} CRUD operations
 */
@WebServlet(urlPatterns = "/stagemonitor/checks")
public class ChecksServlet extends HttpServlet {

	private final AlertingPlugin alertingPlugin;
	private final Configuration configuration;
	private final WebPlugin webPlugin;

	public ChecksServlet() {
		this(Stagemonitor.getConfiguration(AlertingPlugin.class), Stagemonitor.getConfiguration());
	}

	public ChecksServlet(AlertingPlugin alertingPlugin, Configuration configuration) {
		this.alertingPlugin = alertingPlugin;
		this.configuration = configuration;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleCors(resp);
	}

	/**
	 * Returns all checks
	 * <pre>GET /stagemonitor/checks</pre>
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		handleCors(resp);
		resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getOutputStream().print(alertingPlugin.getChecksAsJson());
	}

	private void handleCors(HttpServletResponse resp) {
		if (webPlugin.getMetricsServletAllowedOrigin() != null) {
			resp.setHeader("Access-Control-Allow-Origin", webPlugin.getMetricsServletAllowedOrigin());
			resp.setHeader("Access-Control-Allow-Methods", "GET, PUT, DELETE, OPTIONS");
		}
	}

	/**
	 * Deletes a check
	 * <pre>DELETE /stagemonitor/checks?id={id}&configurationSource={configurationSource}&stagemonitor.password={pwd}</pre>
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleCors(resp);
		Map<String, Check> checks = new LinkedHashMap<String, Check>(alertingPlugin.getChecks());
		if (checks.remove(req.getParameter("id")) == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else {
			save(req, resp, checks);
		}
	}

	/**
	 * Creates or updates a check
	 * <pre>PUT /stagemonitor/checks?id={id}&configurationSource={configurationSource}&stagemonitor.password={pwd}</pre>
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleCors(resp);
		final Check check = JsonUtils.getMapper().readValue(req.getInputStream(), Check.class);
		check.setId(req.getParameter("id"));

		Map<String, Check> checks = new LinkedHashMap<String, Check>(alertingPlugin.getChecks());
		checks.put(check.getId(), check);
		save(req, resp, checks);
	}

	private void save(HttpServletRequest req, HttpServletResponse resp, Map<String, Check> checks) throws IOException {
		ConfigurationServlet.tryToSaveAndHandleErrors(configuration, req, resp,
				req.getParameter(Stagemonitor.STAGEMONITOR_PASSWORD), alertingPlugin.checks.getKey(),
				JsonUtils.toJson(checks));
	}

}
