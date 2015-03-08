package org.stagemonitor.alerting.alerter;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;

@WebServlet(urlPatterns = "/stagemonitor/incidents")
public class IncidentServlet extends HttpServlet {

	private final IncidentRepository incidentRepository;

	public IncidentServlet() {
		this(Stagemonitor.getConfiguration(AlertingPlugin.class));
	}

	public IncidentServlet(AlertingPlugin alertingPlugin) {
		this.incidentRepository = alertingPlugin.getIncidentRepository();
	}

	/**
	 * GET /stagemonitor/incidents
	 *
	 * Returns all current incidents
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JsonUtils.writeJsonToOutputStream(incidentRepository.getAllIncidents(), resp.getOutputStream());
	}

}
