package org.stagemonitor.core.util.http;

import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestBuilder<T> {

	public static final Map<String, String> CONTENT_TYPE_JSON = Collections.singletonMap("Content-Type", "application/json");
	private final String url;
	private String method = "GET";
	private Map<String, String> headers;
	private HttpClient.OutputStreamHandler outputStreamHandler;
	private HttpClient.ResponseHandler<T> successHandler;
	private HttpClient.ResponseHandler<T> errorHandler;
	private Map<Integer, HttpClient.ResponseHandler<T>> statusHandlers;

	private HttpRequestBuilder(String url) {
		this.url = url;
		headers = new HashMap<String, String>();
		successHandler = NoopResponseHandler.getInstance();
		outputStreamHandler = NoopOutputStreamHandler.INSTANCE;
		errorHandler = ErrorLoggingResponseHandler.getInstance();
	}

	public static <T> HttpRequestBuilder<T> forUrl(String url) {
		return new HttpRequestBuilder<T>(url);
	}

	public static <T> HttpRequestBuilder<T> of(String method, String url) {
		return new HttpRequestBuilder<T>(url).method(method);
	}

	public static <T> HttpRequestBuilder<T> jsonRequest(final String method, final String url, final Object requestBody) {
		return HttpRequestBuilder.<T>forUrl(url)
				.method(method)
				.bodyJson(requestBody);
	}

	public static <T> HttpRequestBuilder<T> jsonRequest(final String method, final String url, final String requestBody) {
		return HttpRequestBuilder.<T>forUrl(url)
				.method(method)
				.addHeaders(CONTENT_TYPE_JSON)
				.body(requestBody);
	}

	public HttpRequestBuilder<T> method(String method) {
		this.method = method;
		return this;
	}

	public HttpRequestBuilder<T> addHeaders(Map<String, String> headers) {
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			addHeader(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public HttpRequestBuilder<T> outputStreamHandler(HttpClient.OutputStreamHandler outputStreamHandler) {
		this.outputStreamHandler = outputStreamHandler;
		return this;
	}

	public HttpRequestBuilder<T> successHandler(HttpClient.ResponseHandler<T> responseHandler) {
		this.successHandler = responseHandler;
		return this;
	}

	public HttpRequestBuilder<T> responseHandler(HttpClient.ResponseHandler<T> responseHandler) {
		this.successHandler = responseHandler;
		this.errorHandler = responseHandler;
		return this;
	}

	public HttpRequestBuilder<T> errorHandler(HttpClient.ResponseHandler<T> responseHandler) {
		this.errorHandler = responseHandler;
		return this;
	}

	public HttpRequestBuilder<T> handlerForStatus(int statusCode, HttpClient.ResponseHandler<T> responseHandler) {
		if (this.statusHandlers == null) {
			this.statusHandlers = new HashMap<Integer, HttpClient.ResponseHandler<T>>();
		}
		this.statusHandlers.put(statusCode, responseHandler);
		return this;
	}

	public HttpRequestBuilder<T> noopForStatus(int status) {
		return handlerForStatus(status, NoopResponseHandler.<T>getInstance());
	}

	public HttpRequestBuilder<T> bodyJson(final Object requestBody) {
		this.addHeaders(CONTENT_TYPE_JSON);
		this.outputStreamHandler = new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				JsonUtils.writeJsonToOutputStream(requestBody, os);
			}
		};
		return this;
	}

	public HttpRequestBuilder<T> bodyStream(final InputStream inputStream) {
		this.outputStreamHandler = new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream outputStream) throws IOException {
				IOUtils.copy(inputStream, outputStream);
				inputStream.close();
				outputStream.close();
			}
		};
		return this;
	}

	public HttpRequestBuilder<T> body(final List<String> requestBodyLines) {
		this.outputStreamHandler = new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				for (String line : requestBodyLines) {
					os.write(line.getBytes("UTF-8"));
					os.write('\n');
				}
				os.flush();
			}
		};
		return this;
	}

	public HttpRequestBuilder<T> body(final String requestBody) {
		this.outputStreamHandler = new HttpClient.OutputStreamHandler() {
			@Override
			public void withHttpURLConnection(OutputStream os) throws IOException {
				os.write(requestBody.getBytes("UTF-8"));
				os.flush();
			}
		};
		return this;
	}

	public HttpRequestBuilder<T> addHeader(String key, String value) {
		this.headers.put(key, value);
		return this;
	}

	public HttpRequest<T> build() {
		return new HttpRequestImpl<T>(url, method, headers, outputStreamHandler, new HttpClient.ResponseHandler<T>() {
			@Override
			public T handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
				if (statusHandlers != null && statusHandlers.containsKey(statusCode)) {
					return statusHandlers.get(statusCode).handleResponse(httpRequest, is, statusCode, e);
				} else if (statusCode == null || e != null || statusCode >= 400) {
					return errorHandler.handleResponse(httpRequest, is, statusCode, e);
				} else {
					return successHandler.handleResponse(httpRequest, is, statusCode, e);
				}
			}
		});
	}
}
