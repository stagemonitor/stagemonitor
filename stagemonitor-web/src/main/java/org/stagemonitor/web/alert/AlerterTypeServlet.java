package org.stagemonitor.web.alert;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.alerter.AlerterFactory;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/stagemonitor/alerter-types")
public class AlerterTypeServlet extends HttpServlet {

	private final MeasurementSession measurementSession;
	private final AlerterFactory alerterFactory;

	public AlerterTypeServlet() {
		this(Stagemonitor.getConfiguration(AlertingPlugin.class), Stagemonitor.getMeasurementSession());
	}

	public AlerterTypeServlet(AlertingPlugin alertingPlugin, MeasurementSession measurementSession) {
		this.measurementSession = measurementSession;
		this.alerterFactory = alertingPlugin.getAlerterFactory();
	}

	/**
	 * Returns all available alerters
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JsonUtils.writeJsonToOutputStream(alerterFactory.getAvailableAlerters(), resp.getOutputStream());
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		return measurementSession.getStartTimestampEpoch();
	}
}
