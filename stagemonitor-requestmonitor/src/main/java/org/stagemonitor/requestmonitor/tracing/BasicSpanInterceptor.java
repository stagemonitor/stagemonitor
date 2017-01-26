package org.stagemonitor.requestmonitor.tracing;

import io.opentracing.tag.Tags;

public class BasicSpanInterceptor implements SpanInterceptor {

	protected String operationName;
	protected long startTimestampNanos;
	protected long durationNanos;

	protected boolean isClient;
	protected boolean isRPC;
	protected boolean isServer;

	public BasicSpanInterceptor(String operationName) {
		this.operationName = operationName;
	}

	public void onSetTag(String key, String value) {
		if (key.equals(Tags.SPAN_KIND.getKey())) {
			isClient = Tags.SPAN_KIND_CLIENT.equals(value);
			isServer = Tags.SPAN_KIND_SERVER.equals(value);
			isRPC = isClient || isServer;
		}
	}

	public void onSetTag(String key, boolean value) {
	}

	public void onSetTag(String key, Number value) {
	}

	public void onSetOperationName(String operationName) {
		this.operationName = operationName;
	}

	public void onSetStartTimestamp(long startTimestampNanos) {
		this.startTimestampNanos = startTimestampNanos;
	}

	public void onFinish(long endTimestampNanos) {
		durationNanos = endTimestampNanos - startTimestampNanos;
	}
}
