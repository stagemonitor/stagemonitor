package org.stagemonitor.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public HttpURLConnection sendRequest(final String method, final String url) {
		return sendAsJson(method, url, null, null);
	}

	public HttpURLConnection sendAsJson(final String method, final String url, final Object requestBody) {
		return sendAsJson(method, url, requestBody, null);
	}

	public HttpURLConnection sendAsJson(final String method, final String url, final Object requestBody, final Map<String, String> headerFields) {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod(method);
			if (headerFields != null) {
				for (Map.Entry<String, String> header : headerFields.entrySet()) {
					connection.setRequestProperty(header.getKey(), header.getValue());
				}
			}

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
}
