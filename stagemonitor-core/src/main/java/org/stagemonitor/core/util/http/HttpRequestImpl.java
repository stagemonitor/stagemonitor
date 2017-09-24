package org.stagemonitor.core.util.http;

import org.stagemonitor.core.util.HttpClient;

import java.util.Map;

class HttpRequestImpl<T> implements HttpRequest<T> {

	private final String url;
	private final String method;
	private final Map<String, String> headers;
	private final HttpClient.OutputStreamHandler outputStreamHandler;
	private final HttpClient.ResponseHandler<T> responseHandler;

	HttpRequestImpl(String url, String method, Map<String, String> headers,
					HttpClient.OutputStreamHandler outputStreamHandler, HttpClient.ResponseHandler<T> responseHandler) {
		this.url = url;
		this.method = method;
		this.headers = headers;
		this.outputStreamHandler = outputStreamHandler;
		this.responseHandler = responseHandler;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public Map<String, String> getHeaders() {
		return headers;
	}

	public HttpClient.OutputStreamHandler getOutputStreamHandler() {
		return outputStreamHandler;
	}

	@Override
	public HttpClient.ResponseHandler<T> getResponseHandler() {
		return responseHandler;
	}

	@Override
	public String getSafeUrl() {
		return HttpClient.removeUserInfo(url);
	}
}
