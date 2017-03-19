package org.stagemonitor.requestmonitor.tracing.wrapper;

public class StatelessSpanEventListener extends SpanEventListener implements SpanEventListenerFactory {
	@Override
	public final SpanEventListener create() {
		return this;
	}
}
