package org.stagemonitor.core.util.http;

import org.stagemonitor.core.util.HttpClient;

import java.util.Map;

public interface HttpRequest<T> {
	String getMethod();
	String getUrl();
	Map<String, String> getHeaders();
	HttpClient.OutputStreamHandler getOutputStreamHandler();
	HttpClient.ResponseHandler<T> getResponseHandler();
	String getSafeUrl();
}
