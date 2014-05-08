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

	public static HttpURLConnection sendAsJson(final String urlString, final String method, final Object requestBody) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod(method);
			connection.setRequestProperty("Content-Type", "application/json");

			writeRequestBody(requestBody, connection.getOutputStream());

			connection.getContent();
			final int responseCode = connection.getResponseCode();
			if (responseCode > 400) {
				logger.warn("Received HTTP status {} when executing HTTP request: {} {}", responseCode, method, urlString);
			}
			return connection;
		} catch (IOException e) {
			throw new RuntimeException(e);
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

	public static void sendAsJsonAsync(final String urlString, final String method, final Object requestBody) {
		asyncRestPool.execute(new Runnable() {
			@Override
			public void run() {
				sendAsJson(urlString, method, requestBody);
			}
		});
	}
}
