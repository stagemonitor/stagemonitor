package org.stagemonitor.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.http.HttpRequest;
import org.stagemonitor.core.util.http.HttpRequestBuilder;
import org.stagemonitor.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

public class HttpClient {

	private static final long CONNECT_TIMEOUT_SEC = 5;
	private static final long READ_TIMEOUT_SEC = 15;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void send(String method, String url, final List<String> requestBodyLines) {
		send(HttpRequestBuilder.<Integer>forUrl(url)
				.method(method)
				.body(requestBodyLines)
				.build());
	}

	public <T> T send(final String method, final String url, final Map<String, String> headerFields,
					  OutputStreamHandler outputStreamHandler, ResponseHandler<T> responseHandler) {
		return send(HttpRequestBuilder.<T>forUrl(url)
				.method(method)
				.addHeaders(headerFields)
				.outputStreamHandler(outputStreamHandler)
				.responseHandler(responseHandler)
				.build());
	}

	public <T> T send(final HttpRequest<T> request) {
		HttpURLConnection connection = null;
		InputStream inputStream = null;
		try {
		  	URL parsedUrl = new URL(request.getUrl());
		  	connection = (HttpURLConnection) parsedUrl.openConnection();
			if (parsedUrl.getUserInfo() != null) {
				String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(parsedUrl.getUserInfo().getBytes());
				connection.setRequestProperty("Authorization", basicAuth);
			}
			connection.setDoOutput(true);
			connection.setRequestMethod(request.getMethod());
			connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(CONNECT_TIMEOUT_SEC));
			connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(READ_TIMEOUT_SEC));
			if (request.getHeaders() != null) {
				for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
					connection.setRequestProperty(header.getKey(), header.getValue());
				}
			}

			if (request.getOutputStreamHandler() != null) {
				request.getOutputStreamHandler().withHttpURLConnection(connection.getOutputStream());
			}

			inputStream = connection.getInputStream();

			return request.getResponseHandler().handleResponse(request, inputStream, connection.getResponseCode(), null);
		} catch (IOException e) {
			if (connection != null) {
				inputStream = connection.getErrorStream();
				try {
					return request.getResponseHandler().handleResponse(request, inputStream, getResponseCodeIfPossible(connection), e);
				} catch (IOException e1) {
					logger.warn("Error sending {} request to url {}: {}", request.getMethod(), request.getSafeUrl(), e.getMessage(), e);
					logger.warn("Error handling error response for {} request to url {}: {}", request.getMethod(), request.getSafeUrl(), e1.getMessage(), e1);
					try {
						logger.trace(new String(IOUtils.readToBytes(inputStream), "UTF-8"));
					} catch (IOException e2) {
						logger.trace("Could not read error stream: {}", e2.getMessage(), e2);
					}
				}
			} else {
				logger.warn("Error sending {} request to url {}: {}", request.getMethod(), request.getSafeUrl(), e.getMessage(), e);
			}

			return null;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private Integer getResponseCodeIfPossible(HttpURLConnection connection) {
		try {
			return connection.getResponseCode();
		} catch (IOException e) {
			// don't handle exception twice
			return null;
		}
	}

	public interface OutputStreamHandler {
		void withHttpURLConnection(OutputStream os) throws IOException;
	}

	public interface ResponseHandler<T> {
		T handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException;
	}

	public static String removeUserInfo(String url) {
		String userInfo = "";
		try {
			userInfo = new URL(url).getUserInfo();
		} catch (MalformedURLException e) {
			// ignore
		}
		if (null == userInfo || userInfo.isEmpty()) {
			return url;
		}
		return url.replace(userInfo, "XXXX:XXXX");
	}
}
