package org.stagemonitor.core.elasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchClient.class);
	private static final String STAGEMONITOR_MAJOR_MINOR_VERSION = getStagemonitorMajorMinorVersion();
	private static final String TITLE = "title";
	private static String baseUrl = Stagemonitor.getConfiguration(CorePlugin.class).getElasticsearchUrl();

	public static final ThreadPoolExecutor asyncRestPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("async-elasticsearch");
			return thread;
		}
	});

	static {
		if (Stagemonitor.getConfiguration(CorePlugin.class).isInternalMonitoringActive()) {
			JavaThreadPoolMetricsCollectorImpl pooledResource = new JavaThreadPoolMetricsCollectorImpl(asyncRestPool, "internal.asyncRestPool");
			PooledResourceMetricsRegisterer.registerPooledResource(pooledResource, Stagemonitor.getMetricRegistry());
		}
	}

	public static JsonNode getJson(final String path) throws IOException {
		return JsonUtils.getMapper().readTree(new URL(baseUrl + path).openStream());
	}

	public static void sendAsJson(final String method, final String path, final Object requestBody) {
		if (baseUrl == null || baseUrl.isEmpty()) {
			return;
		}

		HttpURLConnection connection = null;
		try {
			URL url = new URL(baseUrl + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod(method);
			connection.setRequestProperty("Content-Type", "application/json");

			writeRequestBody(requestBody, connection.getOutputStream());

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
	}

	private static void writeRequestBody(Object requestBody, OutputStream os) throws IOException {
		if (requestBody != null && os != null) {
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

	public static Future<?> sendAsJsonAsync(final String method, final String path, final Object requestBody) {
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

	public static Future<?> sendGrafanaDashboardAsync(String dashboardPath) {
		return sendDashboardAsync("/grafana-dash/dashboard/", dashboardPath);
	}

	public static Future<?> sendKibanaDashboardAsync(String dashboardPath) {
		return sendDashboardAsync("/kibana-int/dashboard/", dashboardPath);
	}

	public static Future<?> sendDashboardAsync(String path, String dashboardPath) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			try {
				ObjectNode dashboard = getDashboardForElasticsearch(dashboardPath);
				return ElasticsearchClient.sendAsJsonAsync("PUT", path + slugifyTitle(dashboard) + "/_create", dashboard);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return new CompletedFuture<Object>(null);
	}

	private static String slugifyTitle(ObjectNode dashboard) {
		return dashboard.get(TITLE).asText().toLowerCase().replaceAll("[^\\w ]+", "").replaceAll("\\s+", "-");
	}

	static ObjectNode getDashboardForElasticsearch(String dashboardPath) throws IOException {
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

	private static String getStagemonitorMajorMinorVersion() {
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

	static String getMajorMinorVersionFromFullVersionString(String value) {
		return value.substring(0, value.lastIndexOf('.'));
	}

	public static void setBaseUrl(String baseUrl) {
		ElasticsearchClient.baseUrl = baseUrl;
	}

	private static class CompletedFuture<T> implements Future<T> {
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
		public T get() throws InterruptedException, ExecutionException {
			return this.result;
		}

		@Override
		public T get(final long l, final TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
			return get();
		}
	}

}
