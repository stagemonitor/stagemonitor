package org.stagemonitor.alerting.incident;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * A simple implementation of {@link IncidentRepository} that uses a map as the storage.
 * <p/>
 * Although this is a simple and easy-to-use implementation, incidents from different servers can't be aggregated
 * so you will get alerts for each distinct server.
 * <p/>
 * If you want only one alert per {@link org.stagemonitor.alerting.check.CheckGroup}, even for multiple servers,
 * plug in a implementation that uses a common datasource for all servers such as an elasticsearch cluster,
 * a relational database or a clustered hazelcast map.
 */
public class ConcurrentMapIncidentRepository implements IncidentRepository {

	private final ConcurrentMap<String, Incident> incidentsByCheckGroupId;

	public ConcurrentMapIncidentRepository(ConcurrentMap<String, Incident> incidentsByCheckGroupId) {
		this.incidentsByCheckGroupId = incidentsByCheckGroupId;
	}

	@Override
	public Collection<Incident> getAllIncidents() {
		return incidentsByCheckGroupId.values();
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
