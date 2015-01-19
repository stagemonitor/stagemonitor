package org.stagemonitor.web.alert;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.alerter.Subscription;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.web.configuration.ConfigurationServlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
		JsonUtils.writeJsonToOutputStream(alertingPlugin.getSubscriptions(), resp.getOutputStream());
	}

	/**
	 * Deletes a subscription
	 * <pre>DELETE /stagemonitor/subscriptions?alerterType={alerterType}&configurationSource={configurationSource}&stagemonitor.password={pwd}</pre>
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		final ArrayList<Subscription> subscriptions = new ArrayList<Subscription>(alertingPlugin.getSubscriptions());
		removeSubscriptionWithType(req.getParameter("alerterType"), subscriptions);
		ConfigurationServlet.tryToSaveAndHandleErrors(configuration, req, resp,
				req.getParameter(Stagemonitor.STAGEMONITOR_PASSWORD), alertingPlugin.subscriptions.getKey(),
				JsonUtils.toJson(subscriptions));
	}

	/**
	 * Creates or updates a subscription
	 * <pre>PUT /stagemonitor/subscriptions?alerterType={alerterType}&configurationSource={configurationSource}&stagemonitor.password={pwd}</pre>
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		final Subscription subscription = JsonUtils.getMapper().readValue(req.getInputStream(), Subscription.class);
		final ArrayList<Subscription> subscriptions = new ArrayList<Subscription>(alertingPlugin.getSubscriptions());
		final String alerterType = req.getParameter("alerterType");
		removeSubscriptionWithType(alerterType, subscriptions);
		subscription.setAlerterType(alerterType);
		subscriptions.add(subscription);
		ConfigurationServlet.tryToSaveAndHandleErrors(configuration, req, resp,
				req.getParameter(Stagemonitor.STAGEMONITOR_PASSWORD), alertingPlugin.subscriptions.getKey(),
				JsonUtils.toJson(subscriptions));
	}

	private void removeSubscriptionWithType(String alerterType, ArrayList<Subscription> subscriptions) {
		for (Iterator<Subscription> iterator = subscriptions.iterator(); iterator.hasNext(); ) {
			if (iterator.next().getAlerterType().equals(alerterType)) {
				iterator.remove();
			}
		}
	}
}
