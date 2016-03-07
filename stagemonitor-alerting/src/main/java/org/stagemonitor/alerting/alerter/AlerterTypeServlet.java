package org.stagemonitor.alerting.alerter;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;

public class AlerterTypeServlet extends HttpServlet {

	private final AlertingPlugin alertingPlugin;
	private final MeasurementSession measurementSession;

	public AlerterTypeServlet() {
		this(Stagemonitor.getPlugin(AlertingPlugin.class), Stagemonitor.getMeasurementSession());
	}

	public AlerterTypeServlet(AlertingPlugin alertingPlugin, MeasurementSession measurementSession) {
		this.alertingPlugin = alertingPlugin;
		this.measurementSession = measurementSession;
	}

	/**
	 * Returns all available alerters
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (alertingPlugin.getAlertSender() != null) {
			JsonUtils.writeJsonToOutputStream(alertingPlugin.getAlertSender().getAvailableAlerters(),
					resp.getOutputStream());
		} else {
			JsonUtils.writeJsonToOutputStream(Collections.emptyList(), resp.getOutputStream());
		}
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		return measurementSession.getStartTimestamp();
	}
}
