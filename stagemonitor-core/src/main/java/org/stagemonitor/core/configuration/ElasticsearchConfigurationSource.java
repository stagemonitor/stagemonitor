package org.stagemonitor.core.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.source.AbstractConfigurationSource;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;
import org.stagemonitor.core.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

public class ElasticsearchConfigurationSource extends AbstractConfigurationSource {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfigurationSource.class);
	private final String path;
	private final ElasticsearchClient elasticsearchClient;
	private final String configurationId;
	private ConcurrentMap<String, String> configuration = new ConcurrentHashMap<String, String>();

	public ElasticsearchConfigurationSource(ElasticsearchClient elasticsearchClient, String configurationId) {
		this.elasticsearchClient = elasticsearchClient;
		this.configurationId = configurationId;
		this.path = "/stagemonitor-configuration/configuration/" + this.configurationId;
		try {
			reload();
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	@Override
	public String getValue(String key) {
		return configuration.get(key);
	}

	@Override
	public String getName() {
		return "Elasticsearch (" + configurationId + ")";
	}

	@Override
	public boolean isSavingPossible() {
		return true;
	}

	@Override
	public boolean isSavingPersistent() {
		return true;
	}

	@Override
	public void save(String key, String value) throws IOException {
		final Map<String, String> configToSend = new HashMap<String, String>(configuration);
		configToSend.put(key, value);
		if (elasticsearchClient.isElasticsearchAvailable()) {
			elasticsearchClient.sendAsJson("PUT", path, Collections.singletonMap("configuration", EsConfigurationDto.of(configToSend)));
			configuration.put(key, value);
		} else {
			throw new IOException("Elasticsearch is not available");
		}
	}

	@Override
	public void reload() throws IOException {
		final JsonNode json = elasticsearchClient.getJson(path, true);
		if (json != null) {
			final JsonNode source = json.get("_source").get("configuration");
			List<EsConfigurationDto> configAsList = JsonUtils.getMapper().readValue(source.traverse(), new TypeReference<List<EsConfigurationDto>>() {
			});
			configuration = EsConfigurationDto.toMap(configAsList);
		}
	}

	@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
	private static class EsConfigurationDto {
		final String key;
		final String value;

		@JsonCreator
		private EsConfigurationDto(@JsonProperty("key") String key, @JsonProperty("value") String value) {
			this.key = key;
			this.value = value;
		}

		private static List<EsConfigurationDto> of(Map<String, String> configMap) {
			List<EsConfigurationDto> config = new ArrayList<EsConfigurationDto>(configMap.size());
			for (Map.Entry<String, String> entry : configMap.entrySet()) {
				config.add(new EsConfigurationDto(entry.getKey(), entry.getValue()));
			}
			return config;
		}

		private static ConcurrentMap<String, String> toMap(List<EsConfigurationDto> configAsList) {
			ConcurrentMap<String, String> configAsMap = new ConcurrentHashMap<String, String>();
			for (EsConfigurationDto dto : configAsList) {
				configAsMap.put(dto.key, dto.value);
			}
			return configAsMap;
		}
	}
}
