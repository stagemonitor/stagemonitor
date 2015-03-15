package org.stagemonitor.alerting.alerter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.stagemonitor.alerting.AlertingPlugin;
import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.alerting.incident.IncidentRepository;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.util.JsonUtils;

@WebServlet(urlPatterns = "/stagemonitor/incidents")
public class IncidentServlet extends HttpServlet {

	private final AlertingPlugin alertingPlugin;

	public IncidentServlet() {
		this(Stagemonitor.getConfiguration(AlertingPlugin.class));
	}

	public IncidentServlet(AlertingPlugin alertingPlugin) {
		this.alertingPlugin = alertingPlugin;
	}

	/**
	 * GET /stagemonitor/incidents
	 *
	 * Returns all current incidents
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		IncidentRepository incidentRepository = alertingPlugin.getIncidentRepository();
		if (incidentRepository != null) {
			Map<String, Object> incidents = new HashMap<String, Object>(2, 1);
			Collection<Incident> allIncidents = incidentRepository.getAllIncidents();
			incidents.put("incidents", allIncidents);
			incidents.put("status", Incident.getMostSevereStatus(allIncidents));
			JsonUtils.writeJsonToOutputStream(incidents, resp.getOutputStream());
		} else {
			JsonUtils.writeJsonToOutputStream(Collections.emptyMap(), resp.getOutputStream());
		}
	}

}
