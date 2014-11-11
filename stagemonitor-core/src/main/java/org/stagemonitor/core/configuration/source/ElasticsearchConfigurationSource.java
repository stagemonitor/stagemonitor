package org.stagemonitor.core.configuration.source;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.elasticsearch.ElasticsearchClient;

public class ElasticsearchConfigurationSource extends AbstractConfigurationSource {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final String path;
	private final String configurationId;
	private Map<String, String> configuration = new HashMap<String, String>();

	public ElasticsearchConfigurationSource(String configurationId) {
		this.configurationId = configurationId;
		this.path = "/stagemonitor/configuration/" + this.configurationId;
		reload();
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
		synchronized (this) {
			configuration.put(key, value);
			ElasticsearchClient.sendAsJson("PUT", path, configuration);
		}
	}

	@Override
	public void reload() {
		try {
			final JsonNode source = ElasticsearchClient.getJson(path).get("_source");
			Map<String, String> conf = new HashMap<String, String>((int) Math.ceil(source.size() / 0.75));
			final Iterator<Map.Entry<String, JsonNode>> it = source.fields();
			while (it.hasNext()) {
				Map.Entry<String, JsonNode> next = it.next();
				conf.put(next.getKey(), next.getValue().asText());
			}
			configuration = conf;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
