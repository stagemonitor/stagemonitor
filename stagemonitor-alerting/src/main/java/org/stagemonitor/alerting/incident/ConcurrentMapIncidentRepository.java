package org.stagemonitor.alerting.incident;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A simple implementation of {@link IncidentRepository} that uses a map as the storage.
 * <p/>
 * Although this is a simple and easy-to-use implementation, incidents from different servers can't be aggregated
 * so you will get alerts for each distinct server.
 * <p/>
 * If you want only one alert per {@link org.stagemonitor.alerting.check.Check}, even for multiple servers,
 * plug in a implementation that uses a common datasource for all servers such as an elasticsearch cluster,
 * a relational database or a clustered hazelcast map.
 */
public class ConcurrentMapIncidentRepository implements IncidentRepository {

	private final ConcurrentMap<String, Incident> incidentsByCheckId;

	public ConcurrentMapIncidentRepository() {
		this(new ConcurrentHashMap<String, Incident>());
	}

	public ConcurrentMapIncidentRepository(ConcurrentMap<String, Incident> incidentsByCheckId) {
		this.incidentsByCheckId = incidentsByCheckId;
	}

	@Override
	public Collection<Incident> getAllIncidents() {
		return incidentsByCheckId.values();
	}

	@Override
	public Incident getIncidentByCheckId(String checkId) {
		return incidentsByCheckId.get(checkId);
	}

	@Override
	public boolean deleteIncident(Incident incident) {
		return incidentsByCheckId.remove(incident.getCheckId(), incident.getIncidentWithPreviousVersion());
	}

	@Override
	public boolean createIncident(Incident incident) {
		final Incident previousIncident = incidentsByCheckId.putIfAbsent(incident.getCheckId(), incident);
		return previousIncident == null;
	}

	@Override
	public boolean updateIncident(Incident incident) {
		return incidentsByCheckId.replace(incident.getCheckId(), incident.getIncidentWithPreviousVersion(),
				incident);
	}

}
