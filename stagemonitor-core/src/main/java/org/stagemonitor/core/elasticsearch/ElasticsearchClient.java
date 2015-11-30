package org.stagemonitor.core.elasticsearch;

import static org.stagemonitor.core.util.StringUtils.slugify;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.pool.JavaThreadPoolMetricsCollectorImpl;
import org.stagemonitor.core.pool.PooledResourceMetricsRegisterer;
import org.stagemonitor.core.util.CompletedFuture;
import org.stagemonitor.core.util.DateUtils;
import org.stagemonitor.core.util.ExecutorUtils;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;

public class ElasticsearchClient {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchClient.class);
	private final String TITLE = "title";
	private final HttpClient httpClient;
	private final CorePlugin corePlugin;

	private final ThreadPoolExecutor asyncRestPool;

	public ElasticsearchClient() {
		this(Stagemonitor.getConfiguration().getConfig(CorePlugin.class));
	}

	public ElasticsearchClient(CorePlugin corePlugin) {
		this.corePlugin = corePlugin;
		asyncRestPool = ExecutorUtils
				.createSingleThreadDeamonPool("async-elasticsearch", corePlugin.getThreadPoolQueueCapacityLimit());
		if (corePlugin.isInternalMonitoringActive()) {
			JavaThreadPoolMetricsCollectorImpl pooledResource = new JavaThreadPoolMetricsCollectorImpl(asyncRestPool, "internal.asyncRestPool");
			PooledResourceMetricsRegisterer.registerPooledResource(pooledResource, Stagemonitor.getMetric2Registry());
		}
		this.httpClient = new HttpClient();
	}

	public JsonNode getJson(final String path) throws IOException {
		return JsonUtils.getMapper().readTree(new URL(corePlugin.getElasticsearchUrl() + path).openStream());
	}

	public <T> T getObject(final String path, Class<T> type) {
		try {
			return JsonUtils.getMapper().reader(type).readValue(getJson(path).get("_source"));
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> Collection<T> getAll(String path, int limit, Class<T> clazz) {
		try {
			JsonNode hits = getJson(path + "/_search?size=" + limit).get("hits").get("hits");
			List<T> all = new ArrayList<T>(hits.size());
			ObjectReader reader = JsonUtils.getMapper().reader(clazz);
			for (JsonNode hit : hits) {
				all.add(reader.<T>readValue(hit.get("_source")));
			}
			return all;
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			return Collections.emptyList();
		}
	}

	public int sendRequest(final String method, final String path) {
		return sendAsJson(method, path, null);
	}

	public int sendAsJson(final String method, final String path, final Object requestBody) {
		if (StringUtils.isEmpty(corePlugin.getElasticsearchUrl())) {
			return -1;
		}
		return httpClient.sendAsJson(method, corePlugin.getElasticsearchUrl() + path, requestBody);
	}

	public void index(final String index, final String type, final Object document) {
		sendAsJsonAsync("POST", "/" + index + "/" + type, document);
	}

	public void index(final String index, final String type, String id, final Object document) {
		sendAsJsonAsync("PUT", "/" + index + "/" + type + "/" + id, document);
	}

	public void createIndex(final String index, final InputStream mapping) {
		sendAsJsonAsync("PUT", "/" + index, mapping);
	}

	private Future<?> sendAsJsonAsync(final String method, final String path, final Object requestBody) {
		if (StringUtils.isNotEmpty(corePlugin.getElasticsearchUrl())) {
			try {
				return asyncRestPool.submit(new Runnable() {
					@Override
					public void run() {
						sendAsJson(method, path, requestBody);
					}
				});
			} catch (RejectedExecutionException e) {
				ExecutorUtils.logRejectionWarning(e);
			}
		}
		return new CompletedFuture<Object>(null);
	}

	public Future<?> sendGrafana1DashboardAsync(String dashboardPath) {
		return sendDashboardAsync("/grafana-dash/dashboard/", dashboardPath);
	}

	public Future<?> sendKibanaDashboardAsync(String dashboardPath) {
		return sendDashboardAsync("/kibana-int/dashboard/", dashboardPath);
	}

	public Future<?> sendDashboardAsync(String path, String dashboardPath) {
		if (StringUtils.isNotEmpty(corePlugin.getElasticsearchUrl())) {
			try {
				ObjectNode dashboard = getDashboardForElasticsearch(dashboardPath);
				final String titleSlug = slugify(dashboard.get(TITLE).asText());
				return sendAsJsonAsync("PUT", path + titleSlug, dashboard);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return new CompletedFuture<Object>(null);
	}

	public Future<?> sendMappingTemplateAsync(String templatePath, String templateName) {
		return sendAsJsonAsync("PUT", "/_template/" + templateName, IOUtils.getResourceAsStream(templatePath));
	}

	public void sendBulkAsync(String resource) {
		sendBulkAsync(IOUtils.getResourceAsStream(resource));
	}

	public void sendBulkAsync(final InputStream is) {
		try {
			asyncRestPool.submit(new Runnable() {
				@Override
				public void run() {
					sendBulk(is);
				}
			});
		} catch (RejectedExecutionException e) {
			ExecutorUtils.logRejectionWarning(e);
		}
	}

	public void sendBulk(final InputStream is) {
		if (StringUtils.isEmpty(corePlugin.getElasticsearchUrl())) {
			return;
		}
		httpClient.send("POST", corePlugin.getElasticsearchUrl() + "/_bulk", null, new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				IOUtils.copy(is, os);
				os.close();
			}
		}, new HttpClient.ResponseHandler<Void>() {
			@Override
			public Void handleResponse(InputStream is, Integer statusCode) throws IOException {
				final JsonNode bulkResponse = JsonUtils.getMapper().readTree(is);
				if (bulkResponse.get("errors").booleanValue()) {
					reportBulkErrors(bulkResponse.get("items"));
				}
				return null;
			}
		});
	}

	private void reportBulkErrors(JsonNode items) {
		final StringBuilder sb = new StringBuilder("Error(s) while sending a _bulk request to elasticsearch:");
		for (JsonNode item : items) {
			final JsonNode error = item.get("index").get("error");
			if (error != null) {
				sb.append("\n - ");
				sb.append(error.get("reason").asText());
				if (error.get("type").asText().equals("version_conflict_engine_exception")) {
					sb.append(": Probably you updated a dashboard in Kibana. ")
							.append("Please don't override the stagemonitor dashboards. ")
							.append("If you want to customize a dashboard, save it under a different name. ")
							.append("Stagemonitor will not override your changes, but that also means that you won't ")
							.append("be able to use the latest dashboard enhancements :(. ")
							.append("To resolve this issue, save the updated one under a different name, delete it ")
							.append("and restart stagemonitor so that the dashboard can be recreated.");
				}
			}
		}
		logger.warn(sb.toString());
	}

	public void deleteIndices(String indexPattern) {
		execute("DELETE", indexPattern + "?timeout=20m", "Deleting indices: {}");
	}

	public void optimizeIndices(String indexPattern) {
		execute("POST", indexPattern + "/_optimize?max_num_segments=1&timeout=1h", "Optimizing indices: {}");
	}

	public void updateIndexSettings(String indexPattern, Map<String, ?> settings) {
		final String elasticsearchUrl = corePlugin.getElasticsearchUrl();
		if (StringUtils.isEmpty(elasticsearchUrl)) {
			return;
		}
		final String url = elasticsearchUrl + "/" + indexPattern + "/_settings";
		logger.info("Updating index settings {}\n{}", url, settings);
		httpClient.sendAsJson("PUT", url, settings);
	}

	private void execute(String method, String path, String logMessage) {
		final String elasticsearchUrl = corePlugin.getElasticsearchUrl();
		if (StringUtils.isEmpty(elasticsearchUrl)) {
			return;
		}
		final String url = elasticsearchUrl + "/" + path;
		logger.info(logMessage, url);
		try {
			httpClient.send(method, url);
		} finally {
			logger.info(logMessage, "Done " + url);
		}
	}

	ObjectNode getDashboardForElasticsearch(String dashboardPath) throws IOException {
		final ObjectMapper mapper = JsonUtils.getMapper();
		final ObjectNode dashboard = (ObjectNode) mapper.readTree(IOUtils.getResourceAsStream(dashboardPath));
		dashboard.put("editable", false);
		ObjectNode dashboardElasticsearchFormat = mapper.createObjectNode();
		dashboardElasticsearchFormat.put("user", "guest");
		dashboardElasticsearchFormat.put("group", "guest");
		dashboardElasticsearchFormat.set(TITLE, dashboard.get(TITLE));
		dashboardElasticsearchFormat.set("tags", dashboard.get("tags"));
		dashboardElasticsearchFormat.put("dashboard", dashboard.toString());
		return dashboardElasticsearchFormat;
	}

	public boolean isPoolQueueEmpty() {
		return asyncRestPool.getQueue().isEmpty();
	}

	public void close() {
		asyncRestPool.shutdown();
	}

	/**
	 * Performs an optimize and delete on logstash-style index patterns [prefix]YYYY.MM.DD
	 *
	 * @param indexPrefix the prefix of the logstash-style index pattern
	 */
	public void scheduleIndexManagement(String indexPrefix, int optimizeAndMoveIndicesToColdNodesOlderThanDays, int deleteIndicesOlderThanDays) {
		Timer timer = new Timer(indexPrefix + "elasticsearch-tasks", true);

		if (deleteIndicesOlderThanDays > 0) {
			final TimerTask deleteIndicesTask = new DeleteIndicesTask(corePlugin.getIndexSelector(), indexPrefix,
					deleteIndicesOlderThanDays, this);
			deleteIndicesTask.run();
			timer.schedule(deleteIndicesTask, DateUtils.getNextDateAtHour(0), DateUtils.getDayInMillis());
		}

		if (optimizeAndMoveIndicesToColdNodesOlderThanDays > 0) {
			final TimerTask shardAllocationTask = new ShardAllocationTask(corePlugin.getIndexSelector(), indexPrefix,
					optimizeAndMoveIndicesToColdNodesOlderThanDays, this, "cold");
			shardAllocationTask.run();
			timer.schedule(shardAllocationTask, DateUtils.getNextDateAtHour(0), DateUtils.getDayInMillis());
		}

		if (optimizeAndMoveIndicesToColdNodesOlderThanDays > 0) {
			final TimerTask optimizeIndicesTask = new OptimizeIndicesTask(corePlugin.getIndexSelector(), indexPrefix,
					optimizeAndMoveIndicesToColdNodesOlderThanDays, this);
			timer.schedule(optimizeIndicesTask, DateUtils.getNextDateAtHour(3), DateUtils.getDayInMillis());
		}

	}

}
