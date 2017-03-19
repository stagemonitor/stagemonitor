package org.stagemonitor.requestmonitor;

import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.instrument.CallerUtil;
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
					.asChildOf(currentSpan)
					.start();
		} else {
			// client spans should not be root spans
			span = tracer.buildSpan(callerSignature)
					.withTag(Tags.SAMPLING_PRIORITY.getKey(), (short) 0)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
					.start();
		}
		span.setTag(SpanUtils.OPERATION_TYPE, getType());
		return span;
	}

	protected abstract String getType();

	@Override
	public void execute() throws Exception {
	}
}
