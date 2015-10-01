package org.stagemonitor.core.grafana;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.StringUtils;

/**
 * Utility class for interacting with the Grafana HTTP API
 * <p/>
 * http://docs.grafana.org/reference/http_api/
 */
public class GrafanaClient {

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

	public void createElasticsearchDatasource(final String name, final String url) {
		Map<String, Object> dataSource = new HashMap<String, Object>();
		dataSource.put("name", name);
		dataSource.put("url", url);
		dataSource.put("access", "proxy");
		dataSource.put("database", "[stagemonitor-metrics-]YYYY.MM.DD");
		dataSource.put("isDefault", false);
		dataSource.put("type", "elasticsearch");
		dataSource.put("basicAuth", false);
		Map<String, Object> jsonData = new HashMap<String, Object>();
		jsonData.put("timeField", "@timestamp");
		jsonData.put("interval", "Daily");
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
		final String requestBody = "{\"dashboard\":" + IOUtils.getResourceAsString(classPathLocation) + ",\"overwrite\": false}";
		asyncGrafanaRequest("POST", "/api/dashboards/db", requestBody);
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

}
