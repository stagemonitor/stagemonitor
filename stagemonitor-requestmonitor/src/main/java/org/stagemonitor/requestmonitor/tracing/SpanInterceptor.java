package org.stagemonitor.requestmonitor.tracing;

public interface SpanInterceptor {

	String onSetTag(String key, String value);

	boolean onSetTag(String key, boolean value);

	Number onSetTag(String key, Number value);

	void onSetOperationName(String operationName);

	void onSetStartTimestamp(long startTimestampNanos);

	void onFinish(long endTimestampNanos);
}
