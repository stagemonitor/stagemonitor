package org.stagemonitor.requestmonitor.tracing.jaeger;

import com.uber.jaeger.SpanContext;

import org.slf4j.MDC;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanEventListener;

/**
 * This class adds the {@link MDC} properties requestId, application, host and instance.
 * <p/>
 * If you are using logback or log4j, you can append this to your pattern to append the properties to each log entry:
 * <code>trace:[%X{traceId}] span:[%X{spanId}] A:[%X{application}] H:[%X{host}] I:[%X{instance}]</code>
 */
public class MDCSpanEventListener extends StatelessSpanEventListener {

	private final CorePlugin corePlugin;

	public MDCSpanEventListener() {
		this(Stagemonitor.getPlugin(CorePlugin.class));
	}

	public MDCSpanEventListener(CorePlugin corePlugin) {
		this.corePlugin = corePlugin;
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		if (corePlugin.isStagemonitorActive()) {
			final MeasurementSession measurementSession = corePlugin.getMeasurementSession();
			addToMdcIfNotNull("application", measurementSession.getApplicationName());
			addToMdcIfNotNull("host", measurementSession.getHostName());
			addToMdcIfNotNull("instance", measurementSession.getInstanceName());

			// don't store the context in MDC if stagemonitor is not active
			// so that thread pools that get created on startup don't inherit the ids
			if (Stagemonitor.isStarted() && spanWrapper instanceof SpanWrapper) {
				final com.uber.jaeger.Span jaegerSpan = ((SpanWrapper) spanWrapper).unwrap(com.uber.jaeger.Span.class);
				if (jaegerSpan != null) {
					setContextToMdc(jaegerSpan.context());
				}
			}
		}
	}

	private void setContextToMdc(SpanContext context) {
		addToMdcIfNotZero("traceId", context.getTraceID());
		addToMdcIfNotZero("spanId", context.getSpanID());
		addToMdcIfNotZero("parentId", context.getParentID());
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		// the other keys are not span-scoped hence not removed here
		MDC.remove("traceId");
		MDC.remove("spanId");
		MDC.remove("parentId");
	}

	private void addToMdcIfNotNull(String key, String value) {
		if (value != null) {
			MDC.put(key, value);
		}
	}

	private void addToMdcIfNotZero(String key, long value) {
		if (value != 0) {
			MDC.put(key, Long.toString(value));
		}
	}
}
