package org.stagemonitor.core.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.pool.JavaThreadPoolMetricsCollectorImpl;
import org.stagemonitor.core.pool.PooledResourceMetricsRegisterer;
import org.stagemonitor.core.util.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class RestClient {

	private static final Logger logger = LoggerFactory.getLogger(RestClient.class);
	private static final String STAGEMONITOR_MAJOR_MINOR_VERSION = getStagemonitorMajorMinorVersion();
	private static final String TITLE = "title";

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
		if (StageMonitor.getConfiguration(CorePlugin.class).isInternalMonitoringActive()) {
			JavaThreadPoolMetricsCollectorImpl pooledResource = new JavaThreadPoolMetricsCollectorImpl(asyncRestPool, "internal.asyncRestPool");
			PooledResourceMetricsRegisterer.registerPooledResource(pooledResource, StageMonitor.getMetricRegistry());
		}
	}

	public static void sendAsJson(final String baseUrl, final String path, final String method, final Object requestBody) {
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

	public static void sendAsJsonAsync(final String baseUrl, final String path, final String method, final Object requestBody) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			asyncRestPool.execute(new Runnable() {
				@Override
				public void run() {
					sendAsJson(baseUrl, path, method, requestBody);
				}
			});
		}
	}

	public static  void sendCallStackAsync(final Object requestTrace, final String requestTraceId,
										   final String serverUrl, final String ttl) {
		asyncRestPool.execute(new Runnable() {
			@Override
			public void run() {
				final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
				dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				String path = String.format("/stagemonitor-%s/executions/%s", dateFormat.format(new Date()), requestTraceId);
				if (ttl != null && !ttl.isEmpty()) {
					path += "?ttl=" + ttl;
				}
				sendAsJson(serverUrl, path, "PUT", requestTrace);
			}
		});
	}

	public static void sendGrafanaDashboardAsync(final String baseUrl, String dashboardPath) {
		sendDashboardAsync(baseUrl, "/grafana-dash/dashboard/", dashboardPath);
	}

	public static void sendKibanaDashboardAsync(final String baseUrl, String dashboardPath) {
		sendDashboardAsync(baseUrl, "/kibana-int/dashboard/", dashboardPath);
	}

	public static void sendDashboardAsync(final String baseUrl, String path, String dashboardPath) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			try {
				ObjectNode dashboard = getDashboardForElasticsearch(dashboardPath);
				RestClient.sendAsJsonAsync(baseUrl, path + URLEncoder.encode(dashboard.get(TITLE).asText(), "UTF-8") + "/_create",
						"PUT", dashboard);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	static ObjectNode getDashboardForElasticsearch(String dashboardPath) throws IOException {
		final ObjectMapper mapper = JsonUtils.getMapper();
		final InputStream dashboardStram = RestClient.class.getClassLoader().getResourceAsStream(dashboardPath);
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
		Class clazz = RestClient.class;
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
}
