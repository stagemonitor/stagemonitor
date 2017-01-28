package org.stagemonitor.requestmonitor.tracing.wrapper;

import io.opentracing.Span;

public abstract class SpanInterceptor {

	public String onSetTag(String key, String value) {
		return value;
	}

	public boolean onSetTag(String key, boolean value) {
		return value;
	}

	public Number onSetTag(String key, Number value) {
		return value;
	}

	public void onSetOperationName(String operationName) {
	}

	public void onSetStartTimestamp(long startTimestampNanos) {
	}

	public void onFinish(Span span, long endTimestampNanos) {
	}
}
