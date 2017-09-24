package org.stagemonitor.core.util.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class ErrorLoggingResponseHandler<T> implements HttpClient.ResponseHandler<T> {

	private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingResponseHandler.class);

	private static final ErrorLoggingResponseHandler INSTANCE = new ErrorLoggingResponseHandler();

	public static <T> ErrorLoggingResponseHandler<T> getInstance() {
		return INSTANCE;
	}

	public ErrorLoggingResponseHandler() {
	}

	@Override
	public T handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
		if (statusCode != null && statusCode >= 400) {
			logger.warn(httpRequest.getSafeUrl() + ": " + statusCode + " " + IOUtils.toString(is));
		} else {
			IOUtils.consumeAndClose(is);
		}
		return null;
	}
}
