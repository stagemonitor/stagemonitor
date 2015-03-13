package org.stagemonitor.alerting.incident;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.core.type.TypeReference;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;

public class ElasticsearchIncidentRepository implements IncidentRepository {

	public static final String BASE_URL = "/stagemonitor/incidents/";
	private final ElasticsearchClient elasticsearchClient;

	public ElasticsearchIncidentRepository(ElasticsearchClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public Collection<Incident> getAllIncidents() {
		try {
			return JsonUtils.getMapper().reader(new TypeReference<Collection<Incident>>() {})
					.readValue(elasticsearchClient.getJson(BASE_URL).get(""));
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public Incident getIncidentByCheckId(String checkId) {
		try {
			return JsonUtils.getMapper().reader(Incident.class)
					.readValue(elasticsearchClient.getJson(BASE_URL + checkId).get("_source"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean deleteIncident(Incident incident) {
		return hasNoConflict(elasticsearchClient.sendRequest("DELETE", BASE_URL + incident.getCheckId() + getVersionParameter(incident)));
	}

	@Override
	public boolean createIncident(Incident incident) {
		return hasNoConflict(elasticsearchClient.sendAsJson("PUT", BASE_URL + incident.getCheckId() + "/_create", incident));
	}

	@Override
	public boolean updateIncident(Incident incident) {
		return hasNoConflict(elasticsearchClient.sendAsJson("PUT", BASE_URL + incident.getCheckId() + getVersionParameter(incident), incident));
	}

	private String getVersionParameter(Incident incident) {
		return "?version=" + incident.getVersion();
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
