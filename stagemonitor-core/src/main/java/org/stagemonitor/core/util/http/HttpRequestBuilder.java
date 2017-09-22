package org.stagemonitor.core.util.http;

import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HttpRequestBuilder<T> {

	private String url;
	private String method;
	private Map<String, String> headers;
	private HttpClient.OutputStreamHandler outputStreamHandler;
	private HttpClient.ResponseHandler<T> responseHandler;
	private Set<Integer> errorStatusCodesIgnored;

	public HttpRequestBuilder() {
		headers = new HashMap<String, String>();
		responseHandler = NoopResponseHandler.INSTANCE;
		outputStreamHandler = NoopOutputStreamHandler.INSTANCE;
		errorStatusCodesIgnored = new HashSet<Integer>();
	}

	public HttpRequestBuilder url(String url) {
		this.url = url;
		return this;
	}

	public HttpRequestBuilder method(String method) {
		this.method = method;
		return this;
	}

	public HttpRequestBuilder headers(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public HttpRequestBuilder outputStreamHandler(HttpClient.OutputStreamHandler outputStreamHandler) {
		this.outputStreamHandler = outputStreamHandler;
		return this;
	}

	public HttpRequestBuilder responseHandler(HttpClient.ResponseHandler<T> responseHandler) {
		this.responseHandler = responseHandler;
		return this;
	}

	public HttpRequestBuilder skipErrorLoggingFor(Integer... errorStatusCodesIgnored) {
		this.errorStatusCodesIgnored = new HashSet<Integer>(Arrays.asList(errorStatusCodesIgnored));
		return this;
	}

	public HttpRequestBuilder<T> body(final InputStream inputStream) {
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

	public HttpRequest<T> build() {
		return new HttpRequestImpl<T>(url, method, headers, outputStreamHandler, new HttpClient.ResponseHandler<T>() {
			@Override
			public T handleResponse(InputStream is, Integer statusCode, IOException e) throws IOException {
				if (errorStatusCodesIgnored.contains(statusCode)) {
					IOUtils.consumeAndClose(is);
					return null;
				} else if (statusCode >= 400) {
					new ErrorLoggingResponseHandler(HttpClient.removeUserInfo(url)).handleResponse(is, statusCode, e);
					return null;
				} else {
					return responseHandler.handleResponse(is, statusCode, e);
				}
			}
		});
	}

}
