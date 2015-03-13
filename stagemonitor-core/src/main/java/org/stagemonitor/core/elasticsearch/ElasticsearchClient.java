package org.stagemonitor.core.elasticsearch;

import static org.stagemonitor.core.util.StringUtils.slugify;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.pool.JavaThreadPoolMetricsCollectorImpl;
import org.stagemonitor.core.pool.PooledResourceMetricsRegisterer;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.core.util.JsonUtils;

public class ElasticsearchClient {

	private final Logger logger = LoggerFactory.getLogger(ElasticsearchClient.class);
	private final String STAGEMONITOR_MAJOR_MINOR_VERSION = getStagemonitorMajorMinorVersion();
	private final String TITLE = "title";
	private String baseUrl;

	public final ThreadPoolExecutor asyncRestPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("async-elasticsearch");
			return thread;
		}
	});


	public ElasticsearchClient() {
		this(Stagemonitor.getConfiguration().getConfig(CorePlugin.class));
	}

	public ElasticsearchClient(CorePlugin corePlugin) {
		this(corePlugin.getElasticsearchUrl(), corePlugin.isInternalMonitoringActive());
	}

	public ElasticsearchClient(String baseUrl, boolean internalMonitoringActive) {
		this.baseUrl = baseUrl;
		if (internalMonitoringActive) {
			JavaThreadPoolMetricsCollectorImpl pooledResource = new JavaThreadPoolMetricsCollectorImpl(asyncRestPool, "internal.asyncRestPool");
			PooledResourceMetricsRegisterer.registerPooledResource(pooledResource, Stagemonitor.getMetricRegistry());
		}
	}

	public JsonNode getJson(final String path) throws IOException {
		return JsonUtils.getMapper().readTree(new URL(baseUrl + path).openStream());
	}

	public <T> T getObject(final String path, TypeReference type) throws IOException {
		return JsonUtils.getMapper().reader(type).readValue(getJson(path).get("_source"));
	}

	public HttpURLConnection sendRequest(final String method, final String path) {
		return sendAsJson(method, path, null);
	}

	public HttpURLConnection sendAsJson(final String method, final String path, final Object requestBody) {
		HttpURLConnection connection = null;
		if (baseUrl == null || baseUrl.isEmpty()) {
			return connection;
		}
		try {
			URL url = new URL(baseUrl + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod(method);

			writeRequestBody(requestBody, connection);

			connection.getContent();
		} catch (IOException e) {
			String errorMessage = "";
			if (connection != null) {
				try {
					errorMessage = IOUtils.toString(connection.getErrorStream());
				} catch (IOException e1) {
					logger.warn(e.getMessage(), e);
				}
			}
			logger.warn(e.getMessage() + ": " + errorMessage);
		}
		return connection;
	}

	private void writeRequestBody(Object requestBody, HttpURLConnection connection) throws IOException {
		if (requestBody != null) {
			connection.setRequestProperty("Content-Type", "application/json");
			OutputStream os = connection.getOutputStream();

			if (requestBody instanceof InputStream) {
				byte[] buf = new byte[8192];
				int n;
				while ((n = ((InputStream) requestBody).read(buf)) > 0) {
					os.write(buf, 0, n);
				}
			} else {
				JsonUtils.writeJsonToOutputStream(requestBody, os);
			}
			os.flush();
		}
	}

	public Future<?> sendAsJsonAsync(final String method, final String path, final Object requestBody) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			return asyncRestPool.submit(new Runnable() {
				@Override
				public void run() {
					sendAsJson(method, path, requestBody);
				}
			});
		}
		return new CompletedFuture<Object>(null);
	}

	public Future<?> sendGrafanaDashboardAsync(String dashboardPath) {
		return sendDashboardAsync("/grafana-dash/dashboard/", dashboardPath);
	}

	public Future<?> sendKibanaDashboardAsync(String dashboardPath) {
		return sendDashboardAsync("/kibana-int/dashboard/", dashboardPath);
	}

	public Future<?> sendDashboardAsync(String path, String dashboardPath) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			try {
				ObjectNode dashboard = getDashboardForElasticsearch(dashboardPath);
				final String titleSlug = slugify(dashboard.get(TITLE).asText());
				return sendAsJsonAsync("PUT", path + titleSlug + "/_create", dashboard);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return new CompletedFuture<Object>(null);
	}

	ObjectNode getDashboardForElasticsearch(String dashboardPath) throws IOException {
		final ObjectMapper mapper = JsonUtils.getMapper();
		final InputStream dashboardStram = ElasticsearchClient.class.getClassLoader().getResourceAsStream(dashboardPath);
		final ObjectNode dashboard = (ObjectNode) mapper.readTree(dashboardStram);
		dashboard.put(TITLE, dashboard.get(TITLE).asText() + STAGEMONITOR_MAJOR_MINOR_VERSION);
		ObjectNode dashboardElasticsearchFormat = mapper.createObjectNode();
		dashboardElasticsearchFormat.put("user", "guest");
		dashboardElasticsearchFormat.put("group", "guest");
		dashboardElasticsearchFormat.set(TITLE, dashboard.get(TITLE));
		dashboardElasticsearchFormat.set("tags", dashboard.get("tags"));
		dashboardElasticsearchFormat.put("dashboard", dashboard.toString());
		return dashboardElasticsearchFormat;
	}

	private String getStagemonitorMajorMinorVersion() {
		Class clazz = ElasticsearchClient.class;
		String className = clazz.getSimpleName() + ".class";
		String classPath = clazz.getResource(className).toString();
		if (!classPath.startsWith("jar")) {
			logger.warn("Failed to read stagemonitor version from manifest (class is not in jar)");
			return "";
		}
		String manifestPath = classPath.substring(0, classPath.lastIndexOf('!') + 1) + "/META-INF/MANIFEST.MF";
		try {
			Manifest manifest = new Manifest(new URL(manifestPath).openStream());
			Attributes attr = manifest.getMainAttributes();
			final String value = attr.getValue("Implementation-Version");
			return " " + getMajorMinorVersionFromFullVersionString(value);
		} catch (Exception e) {
			logger.warn("Failed to read stagemonitor version from manifest {}", e.getMessage());
			return "";
		}
	}

	String getMajorMinorVersionFromFullVersionString(String value) {
		return value.substring(0, value.lastIndexOf('.'));
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	private class CompletedFuture<T> implements Future<T> {
		private final T result;

		public CompletedFuture(final T result) {
			this.result = result;
		}

		@Override
		public boolean cancel(final boolean b) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public T get() {
			return this.result;
		}

		@Override
		public T get(final long l, final TimeUnit timeUnit) {
			return get();
		}
	}

}
