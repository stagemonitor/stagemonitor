package org.stagemonitor.core.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.pool.JavaThreadPoolMetricsCollectorImpl;
import org.stagemonitor.core.pool.PooledResourceMetricsRegisterer;
import org.stagemonitor.core.util.JsonUtils;

public class ElasticsearchClient {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchClient.class);
	private static final String STAGEMONITOR_MAJOR_MINOR_VERSION = getStagemonitorMajorMinorVersion();
	private static final String TITLE = "title";
	private static String baseUrl = Stagemonitor.getConfiguration(CorePlugin.class).getElasticsearchUrl();

	private static final ThreadPoolExecutor asyncRestPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("async-rest");
			return thread;
		}
	});

	static {
		if (Stagemonitor.getConfiguration(CorePlugin.class).isInternalMonitoringActive()) {
			JavaThreadPoolMetricsCollectorImpl pooledResource = new JavaThreadPoolMetricsCollectorImpl(asyncRestPool, "internal.asyncRestPool");
			PooledResourceMetricsRegisterer.registerPooledResource(pooledResource, Stagemonitor.getMetricRegistry());
		}
	}

	public static void sendAsJson(final String path, final String method, final Object requestBody) {
		if (baseUrl == null || baseUrl.isEmpty()) {
			return;
		}

		try {
			URL url = new URL(baseUrl + path);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod(method);
			connection.setRequestProperty("Content-Type", "application/json");

			writeRequestBody(requestBody, connection.getOutputStream());

			connection.getContent();
		} catch (IOException e) {
			logger.warn(e.getMessage());
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

	public static void sendAsJsonAsync(final String path, final String method, final Object requestBody) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			asyncRestPool.execute(new Runnable() {
				@Override
				public void run() {
					sendAsJson(path, method, requestBody);
				}
			});
		}
	}

	public static void sendGrafanaDashboardAsync(String dashboardPath) {
		sendDashboardAsync("/grafana-dash/dashboard/", dashboardPath);
	}

	public static void sendKibanaDashboardAsync(String dashboardPath) {
		sendDashboardAsync("/kibana-int/dashboard/", dashboardPath);
	}

	public static void sendDashboardAsync(String path, String dashboardPath) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			try {
				ObjectNode dashboard = getDashboardForElasticsearch(dashboardPath);
				ElasticsearchClient.sendAsJsonAsync(path + slugifyTitle(dashboard) + "/_create",
						"PUT", dashboard);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
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

}
