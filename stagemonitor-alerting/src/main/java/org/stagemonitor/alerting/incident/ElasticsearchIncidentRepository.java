package org.stagemonitor.alerting.incident;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;

import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchIncidentRepository implements IncidentRepository {

	public static final String BASE_URL = "/stagemonitor/incidents/";
	private final ElasticsearchClient elasticsearchClient;

	public ElasticsearchIncidentRepository(ElasticsearchClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public Collection<Incident> getAllIncidents() {
		return elasticsearchClient.getAll(BASE_URL, 100, Incident.class);
	}

	@Override
	public Incident getIncidentByCheckId(String checkId) {
		return elasticsearchClient.getObject(BASE_URL + checkId, Incident.class);
	}

	@Override
	public boolean deleteIncident(Incident incident) {
		return hasNoConflict(elasticsearchClient.sendRequest("DELETE", BASE_URL + incident.getCheckId() + getVersionParameter(incident)));
	}

	@Override
	public boolean createIncident(Incident incident) {
		return hasNoConflict(elasticsearchClient.sendAsJson("PUT", BASE_URL + incident.getCheckId() + "/_create" + getVersionParameter(incident), incident));
	}

	@Override
	public boolean updateIncident(Incident incident) {
		return hasNoConflict(elasticsearchClient.sendAsJson("PUT", BASE_URL + incident.getCheckId() + getVersionParameter(incident), incident));
	}

	private String getVersionParameter(Incident incident) {
		return "?version=" + incident.getVersion() + "&version_type=external";
	}

	private boolean hasNoConflict(HttpURLConnection connection) {
		if (connection == null) {
			return true;
		}
		try {
			return connection.getResponseCode() != 409;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
