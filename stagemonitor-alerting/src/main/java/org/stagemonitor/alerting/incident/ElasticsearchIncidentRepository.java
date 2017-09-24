package org.stagemonitor.alerting.incident;

import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

public class ElasticsearchIncidentRepository implements IncidentRepository {

	public static final String BASE_URL = "/stagemonitor/incidents";
	private ElasticsearchClient elasticsearchClient;

	public ElasticsearchIncidentRepository(ElasticsearchClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public Collection<Incident> getAllIncidents() {
		return elasticsearchClient.getAll(BASE_URL, 100, Incident.class);
	}

	@Override
	public Incident getIncidentByCheckId(String checkId) {
		return elasticsearchClient.getObject(BASE_URL + "/" + getEsId(checkId), Incident.class);
	}

	@Override
	public boolean deleteIncident(Incident incident) {
		return hasNoConflict(elasticsearchClient.delete(BASE_URL + "/" + getEsId(incident) + getVersionParameter(incident)));
	}

	private String getEsId(Incident incident) {
		final String checkId = incident.getCheckId();
		return getEsId(checkId);
	}

	private String getEsId(String checkId) {
		try {
			return URLEncoder.encode(checkId, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean createIncident(Incident incident) {
		if (incident.getVersion() != 1) {
			throw new IllegalArgumentException("Tried to create an incident with version not equal to 1: " + incident.getVersion());
		}
		return updateIncident(incident);
	}

	@Override
	public boolean updateIncident(Incident incident) {
		return hasNoConflict(elasticsearchClient.sendAsJson("PUT", BASE_URL + "/" + getEsId(incident) + getVersionParameter(incident), incident));
	}

	@Override
	public void clear() {
		for (Incident incident : getAllIncidents()) {
			deleteIncident(incident);
		}
	}

	private String getVersionParameter(Incident incident) {
		return "?version=" + incident.getVersion() + "&version_type=external";
	}

	private boolean hasNoConflict(int statusCode) {
		return statusCode != 409;
	}

	void setElasticsearchClient(ElasticsearchClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}
}
