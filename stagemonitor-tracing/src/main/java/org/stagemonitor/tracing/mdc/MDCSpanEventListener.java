package org.stagemonitor.tracing.mdc;

import org.slf4j.MDC;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

/**
 * This class adds the {@link MDC} properties requestId, application, host and instance.
 * <p/>
 * If you are using logback or log4j, you can append this to your pattern to append the properties to each log entry:
 * <code>trace:[%X{traceId}] span:[%X{spanId}] A:[%X{application}] H:[%X{host}] I:[%X{instance}]</code>
 */
public class MDCSpanEventListener extends StatelessSpanEventListener {

	private final CorePlugin corePlugin;
	private final TracingPlugin tracingPlugin;

	public MDCSpanEventListener(CorePlugin corePlugin, TracingPlugin tracingPlugin) {
		this.corePlugin = corePlugin;
		this.tracingPlugin = tracingPlugin;
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		if (corePlugin.isStagemonitorActive()) {
			final MeasurementSession measurementSession = corePlugin.getMeasurementSession();
			if (measurementSession != null) {
				addToMdcIfNotNull("application", measurementSession.getApplicationName());
				addToMdcIfNotNull("host", measurementSession.getHostName());
				addToMdcIfNotNull("instance", measurementSession.getInstanceName());
			}

			// don't store the context in MDC if stagemonitor is not active
			// so that thread pools that get created on startup don't inherit the ids
			if (Stagemonitor.isStarted()) {
				tracingPlugin.getTracer().inject(spanWrapper.context(), B3HeaderFormat.INSTANCE, new B3HeaderFormat.B3InjectAdapter() {
					@Override
					public void setParentId(String value) {
						addToMdcIfNotNull("parentId", value);
					}

					@Override
					public void setSpanId(String value) {
						addToMdcIfNotNull("spanId", value);
					}

					@Override
					public void setTraceId(String value) {
						addToMdcIfNotNull("traceId", value);
					}
				});
			}
		}
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
}
