package org.stagemonitor.collector.core.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
}
