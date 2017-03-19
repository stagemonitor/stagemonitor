package org.stagemonitor.requestmonitor.tracing.wrapper;

public abstract class AbstractSpanEventListener implements SpanEventListener {

	@Override
	public void onStart(SpanWrapper spanWrapper) {
	}

	@Override
	public String onSetTag(String key, String value) {
		return value;
	}

	@Override
	public boolean onSetTag(String key, boolean value) {
		return value;
	}

	@Override
	public Number onSetTag(String key, Number value) {
		return value;
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
	}
}
