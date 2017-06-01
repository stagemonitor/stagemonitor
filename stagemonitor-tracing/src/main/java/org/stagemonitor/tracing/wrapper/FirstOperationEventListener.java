package org.stagemonitor.tracing.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class FirstOperationEventListener extends StatelessSpanEventListener {

	private final AtomicBoolean firstRequest = new AtomicBoolean(true);

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		if (firstRequest.get()) {
			if (customCondition(spanWrapper) && firstRequest.getAndSet(false)) {
				onFirstOperation(spanWrapper);
			}
		}
	}

	public boolean customCondition(SpanWrapper spanWrapper) {
		return true;
	}

	public abstract void onFirstOperation(SpanWrapper spanWrapper);
}
