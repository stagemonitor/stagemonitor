package org.stagemonitor.core.grafana;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.util.IOUtils;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Utility class for interacting with the Grafana HTTP API
 * <p/>
 * http://docs.grafana.org/reference/http_api/
 */
public class GrafanaClient {

	private static final String ES_STAGEMONITOR_DS_NAME = "ES stagemonitor";
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final ThreadPoolExecutor asyncRestPool;

	private final CorePlugin corePlugin;
	private final HttpClient httpClient;

	public GrafanaClient(CorePlugin corePlugin) {
		this(corePlugin, new HttpClient());
	}

	public GrafanaClient(CorePlugin corePlugin, HttpClient httpClient) {
		this.corePlugin = corePlugin;
		this.httpClient = httpClient;
		asyncRestPool = ExecutorUtils
				.createSingleThreadDeamonPool("async-grafana", corePlugin.getThreadPoolQueueCapacityLimit());
	}

	public void createElasticsearchDatasource(final String url) {
		Map<String, Object> dataSource = new HashMap<String, Object>();
		dataSource.put("name", ES_STAGEMONITOR_DS_NAME);
		dataSource.put("url", url);
		dataSource.put("access", "proxy");
		dataSource.put("database", "[stagemonitor-metrics-]YYYY.MM.DD");
		dataSource.put("isDefault", false);
		dataSource.put("type", "elasticsearch");
		dataSource.put("basicAuth", false);
		Map<String, Object> jsonData = new HashMap<String, Object>();
		jsonData.put("timeField", "@timestamp");
		jsonData.put("interval", "Daily");
		jsonData.put("timeInterval", ">" + corePlugin.getElasticsearchReportingInterval() + "s");
		jsonData.put("esVersion", 5);
		dataSource.put("jsonData", jsonData);
		asyncGrafanaRequest("POST", "/api/datasources", dataSource);
	}

	/**
	 * Saves a dashboard to Grafana
	 * <p/>
	 * If the Grafana url or the API Key is not configured, this method does nothing.
	 *
	 * @param classPathLocation The location of the dashboard
	 */
	public void sendGrafanaDashboardAsync(final String classPathLocation) {
		try {
			final ObjectNode dashboard = (ObjectNode) JsonUtils.getMapper().readTree(IOUtils.getResourceAsStream(classPathLocation));
			dashboard.put("editable", false);
			addMinIntervalToPanels(dashboard, corePlugin.getElasticsearchReportingInterval() + "s");
			final String requestBody = "{\"dashboard\":" + dashboard + ",\"overwrite\": true}";
			asyncGrafanaRequest("POST", "/api/dashboards/db", requestBody);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	private void addMinIntervalToPanels(ObjectNode dashboard, String interval) {
		for (JsonNode row : dashboard.get("rows")) {
			for (JsonNode panel : row.get("panels")) {
				if (panel.has("datasource") && panel.get("datasource").asText().equals(ES_STAGEMONITOR_DS_NAME)) {
					((ObjectNode) panel).put("interval", "$Interval");
				}
			}
		}
		for (JsonNode template : dashboard.get("templating").get("list")) {
			if (template.has("name") && "Interval".equals(template.get("name").asText())) {
				((ObjectNode) template).put("auto_min", interval);
			}
		}
	}

	private void asyncGrafanaRequest(final String method, final String path, final Object requestBody) {
		final String grafanaUrl = corePlugin.getGrafanaUrl();
		final String grafanaApiToken = corePlugin.getGrafanaApiKey();
		if (isGrafanaConfigured(grafanaUrl, grafanaApiToken)) {
			try {
				asyncRestPool.submit(new Runnable() {
					@Override
					public void run() {
						final Map<String, String> authHeader = Collections.singletonMap("Authorization", "Bearer " + grafanaApiToken);
						httpClient.sendAsJson(method, grafanaUrl + path, requestBody, authHeader);
					}
				});
			} catch (RejectedExecutionException e) {
				ExecutorUtils.logRejectionWarning(e);
			}
		} else {
			logger.debug("Not requesting grafana, because the url or the api key is not configured.");
		}
	}

	private boolean isGrafanaConfigured(String grafanaUrl, String apiToken) {
		return StringUtils.isNotEmpty(grafanaUrl) && StringUtils.isNotEmpty(apiToken);
	}

	public void close() {
		asyncRestPool.shutdown();
	}

	public void waitForCompletion() throws ExecutionException, InterruptedException {
		// because the pool is single threaded,
		// all previously submitted tasks are completed when this task finishes
		asyncRestPool.submit(new Runnable() {
			public void run() {
			}
		}).get();
	}

}
