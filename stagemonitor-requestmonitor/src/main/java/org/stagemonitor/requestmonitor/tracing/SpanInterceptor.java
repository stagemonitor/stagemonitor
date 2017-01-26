package org.stagemonitor.requestmonitor.tracing;

public interface SpanInterceptor {

	void onSetTag(String key, String value);

	void onSetTag(String key, boolean value);

	void onSetTag(String key, Number value);

	void onSetOperationName(String operationName);

	void onSetStartTimestamp(long startTimestampNanos);

	void onFinish(long endTimestampNanos);
}
