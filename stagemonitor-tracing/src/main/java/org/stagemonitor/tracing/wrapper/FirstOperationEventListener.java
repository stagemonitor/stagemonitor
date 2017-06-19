package org.stagemonitor.tracing.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FirstOperationEventListener extends StatelessSpanEventListener {

	private final AtomicBoolean firstRequest = new AtomicBoolean(true);
	private final SpanWrappingTracer spanWrappingTracer;

	protected FirstOperationEventListener(SpanWrappingTracer spanWrappingTracer) {
		this.spanWrappingTracer = spanWrappingTracer;
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		if (firstRequest.get()) {
			if (customCondition(spanWrapper) && firstRequest.getAndSet(false)) {
				onFirstOperation(spanWrapper);
				if (spanWrappingTracer != null) {
					spanWrappingTracer.removeEventListenerFactory(this);
				}
			}
		}
	}

	public boolean customCondition(SpanWrapper spanWrapper) {
		return true;
	}

	public abstract void onFirstOperation(SpanWrapper spanWrapper);
}
