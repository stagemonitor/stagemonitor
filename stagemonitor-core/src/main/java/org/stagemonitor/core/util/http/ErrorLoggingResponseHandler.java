package org.stagemonitor.core.util.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class ErrorLoggingResponseHandler implements HttpClient.ResponseHandler<Integer> {

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
