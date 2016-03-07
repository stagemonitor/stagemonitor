package org.stagemonitor.alerting.alerter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.check.CheckResult;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;

public class TestAlertSenderServlet extends HttpServlet {

	private final AlertingPlugin alertingPlugin;

	public TestAlertSenderServlet() {
		this(Stagemonitor.getPlugin(AlertingPlugin.class));
	}

	public TestAlertSenderServlet(AlertingPlugin alertingPlugin) {
		this.alertingPlugin = alertingPlugin;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final CheckResult.Status status = CheckResult.Status.valueOf(req.getParameter("status"));
		final Subscription subscription = JsonUtils.getMapper().readValue(req.getInputStream(), Subscription.class);

		alertingPlugin.getAlertSender().sendTestAlert(subscription, status);
	}
}
