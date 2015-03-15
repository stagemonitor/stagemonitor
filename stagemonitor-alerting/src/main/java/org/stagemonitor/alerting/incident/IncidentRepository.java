package org.stagemonitor.alerting.incident;

import java.util.Collection;

public interface IncidentRepository {

	Collection<Incident> getAllIncidents();

	Incident getIncidentByCheckId(String checkId);

	boolean deleteIncident(Incident incident);

	boolean createIncident(Incident incident);

	boolean updateIncident(Incident incident);

}
