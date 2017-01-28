package org.stagemonitor.requestmonitor.tracing.wrapper;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class BasicSpanInterceptor extends SpanInterceptor {

	protected String operationName;
	protected long startTimestampNanos;
	protected long durationNanos;

	protected boolean isClient;
	protected boolean isRPC;
	protected boolean isServer;

	public BasicSpanInterceptor(String operationName) {
		this.operationName = operationName;
	}

	public String onSetTag(String key, String value) {
		if (key.equals(Tags.SPAN_KIND.getKey())) {
			isClient = Tags.SPAN_KIND_CLIENT.equals(value);
			isServer = Tags.SPAN_KIND_SERVER.equals(value);
			isRPC = isClient || isServer;
		}
		return value;
	}

	public boolean onSetTag(String key, boolean value) {
		return value;
	}

	public Number onSetTag(String key, Number value) {
		return value;
	}

	public void onSetOperationName(String operationName) {
		this.operationName = operationName;
	}

	public void onSetStartTimestamp(long startTimestampNanos) {
		this.startTimestampNanos = startTimestampNanos;
	}

	public void onFinish(Span span, long endTimestampNanos) {
		durationNanos = endTimestampNanos - startTimestampNanos;
	}
}
