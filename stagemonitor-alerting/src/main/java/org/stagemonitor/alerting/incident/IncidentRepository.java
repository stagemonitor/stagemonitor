package org.stagemonitor.alerting.incident;

public interface IncidentRepository {

	Incident getIncidentByCheckGroupId(String checkGroupId);

	boolean deleteIncident(Incident incident);

	boolean createIncident(Incident incident);

	boolean updateIncident(Incident incident);

}
