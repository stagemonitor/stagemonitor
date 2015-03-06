package org.stagemonitor.web.alert;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.alerter.Subscription;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.web.configuration.ConfigurationServlet;

/**
 * A servlet that handles subscription CRUD operations
 */
@WebServlet(urlPatterns = "/stagemonitor/subscriptions")
public class SubscriptionServlet extends HttpServlet {

	private final AlertingPlugin alertingPlugin;
	private final Configuration configuration;

	public SubscriptionServlet() {
		this(Stagemonitor.getConfiguration(AlertingPlugin.class), Stagemonitor.getConfiguration());
	}

	public SubscriptionServlet(AlertingPlugin alertingPlugin, Configuration configuration) {
		this.alertingPlugin = alertingPlugin;
		this.configuration = configuration;
	}

	/**
	 * Returns all subscriptions
	 * <pre>GET /stagemonitor/subscriptions</pre>
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.getOutputStream().print(alertingPlugin.getSubscriptionsByIdsAsJson());
	}

	/**
	 * Deletes a subscription
	 * <pre>DELETE /stagemonitor/subscriptions?id={id}&configurationSource={configurationSource}&stagemonitor.password={pwd}</pre>
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
