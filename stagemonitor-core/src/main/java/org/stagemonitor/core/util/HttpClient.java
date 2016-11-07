package org.stagemonitor.core.util;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// TODO create HttpRequest POJO
// method, url, headers, outputStreamHandler, responseHandler
// builder methods logErrors(int... excludedStatusCodes)
public class HttpClient {

	private static final long CONNECT_TIMEOUT_SEC = 5;
	private static final long READ_TIMEOUT_SEC = 15;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public int send(final String method, final String url) {
		return send(method, url, null, null);
	}

	public JsonNode getJson(String url, Map<String, String> headers) {
		headers = new HashMap<String, String>(headers);
		headers.put("Accept", "application/json");
		return send("GET", url, headers, null, new ResponseHandler<JsonNode>() {
			@Override
			public JsonNode handleResponse(InputStream is, Integer statusCode, IOException e) throws IOException {
				return JsonUtils.getMapper().readTree(is);
			}
		});
	}

	public int sendAsJson(final String method, final String url, final Object requestBody) {
		return sendAsJson(method, url, requestBody, new HashMap<String, String>());
	}

	public int sendAsJson(final String method, final String url, final Object requestBody, Map<String, String> headerFields) {
		headerFields = new HashMap<String, String>(headerFields);
		headerFields.put("Content-Type", "application/json");
		return send(method, url, headerFields, new OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				writeRequestBody(requestBody, os);
			}
		});
	}

	public int send(String method, String url, final List<String> requestBodyLines) {

		return send(method, url, null, new OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				for (String line : requestBodyLines) {
					os.write(line.getBytes("UTF-8"));
					os.write('\n');
				}
				os.flush();
			}
		});
	}

	public int send(final String method, final String url, final Map<String, String> headerFields, OutputStreamHandler outputStreamHandler) {
		return send(method, url, headerFields, outputStreamHandler, new ErrorLoggingResponseHandler(url));
	}

	public <T> T send(final String method, final String url, final Map<String, String> headerFields,
					  OutputStreamHandler outputStreamHandler, ResponseHandler<T> responseHandler) {

		HttpURLConnection connection = null;
		InputStream inputStream = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod(method);
			connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(CONNECT_TIMEOUT_SEC));
			connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(READ_TIMEOUT_SEC));
			if (headerFields != null) {
				for (Map.Entry<String, String> header : headerFields.entrySet()) {
					connection.setRequestProperty(header.getKey(), header.getValue());
				}
			}

			if (outputStreamHandler != null) {
				outputStreamHandler.withHttpURLConnection(connection.getOutputStream());
			}

			inputStream = connection.getInputStream();

			return responseHandler.handleResponse(inputStream, connection.getResponseCode(), null);
		} catch (IOException e) {
			if (connection != null) {
				inputStream = connection.getErrorStream();
				try {
					return responseHandler.handleResponse(inputStream, getResponseCode(method, url, connection), e);
				} catch (IOException e1) {
					logger.warn("Error sending {} request to url {}: {}", method, url, e.getMessage(), e);
					logger.warn("Error handling error response for {} request to url {}: {}", method, url, e1.getMessage(), e1);
					try {
						logger.trace(new String(IOUtils.readToBytes(inputStream), "UTF-8"));
					} catch (IOException e2) {
						logger.trace("Could not read error stream: {}", e2.getMessage(), e2);
					}
				}
			} else {
				logger.warn("Error sending {} request to url {}: {}", method, url, e.getMessage(), e);
			}

			return null;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private Integer getResponseCode(String method, String url, HttpURLConnection connection) {
		try {
			return connection.getResponseCode();
		} catch (IOException e) {
			logger.warn("Error getting response code for {} request to url {}: {}", method, url, e.getMessage(), e);
			return null;
		}
	}

	private void writeRequestBody(Object requestBody, OutputStream os) throws IOException {
		if (requestBody != null) {
			if (requestBody instanceof InputStream) {
				IOUtils.copy((InputStream) requestBody, os);
			} else if (requestBody instanceof String) {
				os.write(((String)requestBody).getBytes("UTF-8"));
			} else {
				JsonUtils.writeJsonToOutputStream(requestBody, os);
			}
			os.flush();
		}
	}

	public interface OutputStreamHandler {
		void withHttpURLConnection(OutputStream os) throws IOException;
	}

	public interface ResponseHandler<T> {
		T handleResponse(InputStream is, Integer statusCode, IOException e) throws IOException;
	}

	private static class ErrorLoggingResponseHandler implements ResponseHandler<Integer> {

		private final Logger logger = LoggerFactory.getLogger(getClass());

		private final String url;

		public ErrorLoggingResponseHandler(String url) {
			this.url = url;
		}

		@Override
		public Integer handleResponse(InputStream is, Integer statusCode, IOException e) throws IOException {
			if (statusCode == null) {
				return -1;
			}
			if (statusCode >= 400) {
				logger.warn(url + ": " + statusCode + " " + IOUtils.toString(is));
			} else {
				IOUtils.consumeAndClose(is);
			}
			return statusCode;
		}
	}
}
