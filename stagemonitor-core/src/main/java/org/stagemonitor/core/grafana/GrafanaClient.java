package org.stagemonitor.core.grafana;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.util.CompletedFuture;
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

	public final ThreadPoolExecutor asyncRestPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("async-grafana");
			return thread;
		}
	});

	private final CorePlugin corePlugin;
	private final HttpClient httpClient;

	public GrafanaClient(CorePlugin corePlugin) {
		this(corePlugin, new HttpClient());
	}

	public GrafanaClient(CorePlugin corePlugin, HttpClient httpClient) {
		this.corePlugin = corePlugin;
		this.httpClient = httpClient;
	}

	public Future<Integer> sendGrafanaDashboardAsync(final String classPathLocation) {
		final String grafanaUrl = corePlugin.getGrafanaUrl();
		final String grafanaApiToken = corePlugin.getGrafanaApiKey();
		if (isGrafanaConfigured(grafanaUrl, grafanaApiToken)) {
			return asyncRestPool.submit(new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					final String requestBody = "{\"dashboard\":" + IOUtils.getResourceAsString(classPathLocation) + ",\"overwrite\": false}";
					final Map<String, String> authHeader = Collections.singletonMap("Authorization", "Bearer " + grafanaApiToken);
					return httpClient.sendAsJson("POST", grafanaUrl + "/api/dashboards/db", requestBody, authHeader);
				}
			});
		} else {
			logger.debug("Not sending dashboard " + classPathLocation + " to grafana, because the url or the api key is not configured.");
			return new CompletedFuture<Integer>(-1);
		}
	}

	private boolean isGrafanaConfigured(String grafanaUrl, String apiToken) {
		return StringUtils.isNotEmpty(grafanaUrl) && StringUtils.isNotEmpty(apiToken);
	}

	public void close() {
		asyncRestPool.shutdown();
	}

}
