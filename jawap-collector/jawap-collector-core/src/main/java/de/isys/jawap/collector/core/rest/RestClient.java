package de.isys.jawap.collector.core.rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RestClient {

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

			OutputStream os = connection.getOutputStream();
			MAPPER.writeValue(os, requestBody);
			os.flush();

			connection.getContent();
			final int responseCode = connection.getResponseCode();
			if (responseCode > 400) {
				throw new RuntimeException("Received HTTP status " + responseCode);
			}
			return connection;
		} catch (IOException e) {
			throw new RuntimeException(e);
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
