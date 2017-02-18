package org.stagemonitor.requestmonitor;

import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.instrument.CallerUtil;
import org.stagemonitor.requestmonitor.tracing.NoopSpan;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public abstract class AbstractExternalRequest extends MonitoredRequest {

	private final RequestMonitorPlugin requestMonitorPlugin;

	protected AbstractExternalRequest(RequestMonitorPlugin requestMonitorPlugin) {
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	public Span createSpan() {
		final Tracer tracer = requestMonitorPlugin.getTracer();
		final String callerSignature = CallerUtil.getCallerSignature();
		final Span span;
		if (!TracingUtils.getTraceContext().isEmpty()) {
			final Span currentSpan = TracingUtils.getTraceContext().getCurrentSpan();
			span = tracer.buildSpan(callerSignature)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
					.asChildOf(currentSpan).start();
		} else {
			// client spans should not be root spans
			span = NoopSpan.INSTANCE;
		}
		SpanUtils.setOperationType(span, getType());
		return span;
	}

	protected abstract String getType();

	public boolean isMonitorForwardedExecutions() {
		return true;
	}

	@Override
	public Object execute() throws Exception {
		return null;
	}
}
