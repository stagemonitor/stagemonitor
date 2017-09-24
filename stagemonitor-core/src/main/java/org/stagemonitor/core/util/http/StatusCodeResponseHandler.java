package org.stagemonitor.core.util.http;

import org.stagemonitor.core.util.HttpClient;

import java.io.IOException;
import java.io.InputStream;

public class StatusCodeResponseHandler implements HttpClient.ResponseHandler<Integer> {

	public static final StatusCodeResponseHandler WITH_ERROR_LOGGING = new StatusCodeResponseHandler(ErrorLoggingResponseHandler.getInstance());

	private final HttpClient.ResponseHandler<?> responseHandler;

	public StatusCodeResponseHandler() {
		responseHandler = NoopResponseHandler.INSTANCE;
	}

	public StatusCodeResponseHandler(HttpClient.ResponseHandler<?> responseHandler) {
		this.responseHandler = responseHandler;
	}

	@Override
	public Integer handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
		responseHandler.handleResponse(httpRequest, is, statusCode, e);
		if (statusCode == null) {
			return -1;
		}
		return statusCode;
	}
}
