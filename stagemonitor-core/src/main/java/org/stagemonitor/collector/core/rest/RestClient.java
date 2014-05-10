package org.stagemonitor.collector.core.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RestClient {

	private final static Logger logger = LoggerFactory.getLogger(RestClient.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final ExecutorService asyncRestPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("async-rest");
			return thread;
		}
	});

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
				MAPPER.writeValue(os, requestBody);
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
				RestClient.sendAsJsonAsync(baseUrl, path + URLEncoder.encode(dashboard.get("title").asText(), "UTF-8") + "/_create",
						"PUT", dashboard);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	static ObjectNode getDashboardForElasticsearch(String dashboardPath) throws IOException {
		final InputStream dashboardStram = RestClient.class.getClassLoader().getResourceAsStream(dashboardPath);
		final JsonNode dashboard = MAPPER.readTree(dashboardStram);
		ObjectNode dashboardElasticsearchFormat = MAPPER.createObjectNode();
		dashboardElasticsearchFormat.put("user", "guest");
		dashboardElasticsearchFormat.put("group", "guest");
		dashboardElasticsearchFormat.put("title", dashboard.get("title"));
		dashboardElasticsearchFormat.put("tags", dashboard.get("tags"));
		dashboardElasticsearchFormat.put("dashboard", dashboard.toString());
		return dashboardElasticsearchFormat;
	}
}
