package org.stagemonitor.alerting;

import org.stagemonitor.alerting.incident.Incident;
import org.stagemonitor.alerting.incident.IncidentRepository;

import java.util.concurrent.ConcurrentMap;

public class ConcurrentMapIncidentRepository implements IncidentRepository {

	private final ConcurrentMap<String, Incident> incidentsByCheckGroupId;

	public ConcurrentMapIncidentRepository(ConcurrentMap<String, Incident> incidentsByCheckGroupId) {
		this.incidentsByCheckGroupId = incidentsByCheckGroupId;
	}

	@Override
	public Incident getIncidentByCheckGroupId(String checkGroupId) {
		return incidentsByCheckGroupId.get(checkGroupId);
	}

	@Override
	public boolean deleteIncident(Incident incident) {
		return incidentsByCheckGroupId.remove(incident.getCheckGroupId(), incident.getIncidentWithPreviousVersion());
	}

	@Override
	public boolean createIncident(Incident incident) {
		final Incident previousIncident = incidentsByCheckGroupId.putIfAbsent(incident.getCheckGroupId(), incident);
		return previousIncident == null;
	}

	@Override
	public boolean updateIncident(Incident incident) {
		return incidentsByCheckGroupId.replace(incident.getCheckGroupId(), incident.getIncidentWithPreviousVersion(),
				incident);
	}

}
