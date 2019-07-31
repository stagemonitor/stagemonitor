package org.stagemonitor.web.servlet.filter;

import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

class SpanWrapperCaptor extends StatelessSpanEventListener {

	private static final ThreadLocal<SpanWrapper> spanWrapperThreadLocal = new ThreadLocal<SpanWrapper>();

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		if ("http".equals(spanWrapper.getTags().get(SpanUtils.OPERATION_TYPE))) {
			spanWrapperThreadLocal.set(spanWrapper);
		}
	}

	SpanWrapper getSpanWrapper() {
		return spanWrapperThreadLocal.get();
	}

	void clear() {
		spanWrapperThreadLocal.remove();
	}
}
