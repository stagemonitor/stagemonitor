package org.stagemonitor.core.util.http;

import org.stagemonitor.core.util.HttpClient;
import org.stagemonitor.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class NoopResponseHandler<T> implements HttpClient.ResponseHandler<T> {

	public static final NoopResponseHandler INSTANCE = new NoopResponseHandler();

	public static <T> HttpClient.ResponseHandler<T> getInstance() {
		return INSTANCE;
	}

	private NoopResponseHandler() {
	}

	@Override
	public T handleResponse(HttpRequest<?> httpRequest, InputStream is, Integer statusCode, IOException e) throws IOException {
		// we have to read the whole response, otherwise bad things happen
		IOUtils.consumeAndClose(is);
		return null;
	}

}
