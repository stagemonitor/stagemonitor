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
import org.stagemonitor.alerting.alerter.Subscription;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.configuration.ConfigurationServlet;

/**
 * A servlet that handles subscription CRUD operations
 */
@WebServlet(urlPatterns = "/stagemonitor/subscriptions")
public class SubscriptionServlet extends HttpServlet {

	private final AlertingPlugin alertingPlugin;
	private final Configuration configuration;
	private final WebPlugin webPlugin;

	public SubscriptionServlet() {
		this(Stagemonitor.getConfiguration(AlertingPlugin.class), Stagemonitor.getConfiguration());
	}

	public SubscriptionServlet(AlertingPlugin alertingPlugin, Configuration configuration) {
		this.alertingPlugin = alertingPlugin;
		this.configuration = configuration;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleCors(resp);
	}

	/**
	 * Returns all subscriptions
	 * <pre>GET /stagemonitor/subscriptions</pre>
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		handleCors(resp);
		resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getOutputStream().print(alertingPlugin.getSubscriptionsByIdsAsJson());
	}

	private void handleCors(HttpServletResponse resp) {
		if (webPlugin.getMetricsServletAllowedOrigin() != null) {
			resp.setHeader("Access-Control-Allow-Origin", webPlugin.getMetricsServletAllowedOrigin());
			resp.setHeader("Access-Control-Allow-Methods", "GET, PUT, DELETE, OPTIONS");
		}
	}

	/**
	 * Deletes a subscription
	 * <pre>DELETE /stagemonitor/subscriptions?id={id}&configurationSource={configurationSource}&stagemonitor.password={pwd}</pre>
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleCors(resp);
		Map<String, Subscription> subscriptions = new LinkedHashMap<String, Subscription>(alertingPlugin.getSubscriptionsByIds());
		if (subscriptions.remove(req.getParameter("id")) == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else {
			save(req, resp, subscriptions);
		}
	}

	/**
	 * Creates or updates a subscription
	 * <pre>PUT /stagemonitor/subscriptions?id={id}&configurationSource={configurationSource}&stagemonitor.password={pwd}</pre>
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleCors(resp);
		final Subscription subscription = JsonUtils.getMapper().readValue(req.getInputStream(), Subscription.class);
		subscription.setId(req.getParameter("id"));

		Map<String, Subscription> subscriptions = new LinkedHashMap<String, Subscription>(alertingPlugin.getSubscriptionsByIds());
		subscriptions.put(subscription.getId(), subscription);
		save(req, resp, subscriptions);
	}

	private void save(HttpServletRequest req, HttpServletResponse resp, Map<String, Subscription> subscriptions) throws IOException {
		ConfigurationServlet.tryToSaveAndHandleErrors(configuration, req, resp,
				req.getParameter(Stagemonitor.STAGEMONITOR_PASSWORD), alertingPlugin.subscriptions.getKey(),
				JsonUtils.toJson(subscriptions));
	}

}
