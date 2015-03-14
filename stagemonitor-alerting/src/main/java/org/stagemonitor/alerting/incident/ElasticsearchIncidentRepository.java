package org.stagemonitor.alerting.incident;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;

public class ElasticsearchIncidentRepository implements IncidentRepository {

	public static final ObjectReader READER = JsonUtils.getMapper().reader(Incident.class);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String BASE_URL = "/stagemonitor/incidents/";
	private final ElasticsearchClient elasticsearchClient;

	public ElasticsearchIncidentRepository(ElasticsearchClient elasticsearchClient) {
		this.elasticsearchClient = elasticsearchClient;
	}

	@Override
	public Collection<Incident> getAllIncidents() {
		try {
			JsonNode hits = elasticsearchClient.getJson(BASE_URL + "/_search").get("hits").get("hits");
			List<Incident> incidents = new ArrayList<Incident>(hits.size());
			for (JsonNode hit : hits) {
				incidents.add(asIncident(hit));
			}
			return incidents;
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public Incident getIncidentByCheckId(String checkId) {
		try {
			return asIncident(elasticsearchClient.getJson(BASE_URL + checkId));
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

	private Incident asIncident(JsonNode hit) throws IOException {
		return READER.readValue(hit.get("_source"));
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
