package org.stagemonitor.requestmonitor;

import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.instrument.CallerUtil;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public abstract class AbstractExternalRequest extends MonitoredRequest {

	private final Tracer tracer;
	private final String operationName;

	protected AbstractExternalRequest(Tracer tracer) {
		this(tracer, CallerUtil.getCallerSignature());
	}

	public AbstractExternalRequest(Tracer tracer, String operationName) {
		this.tracer = tracer;
		this.operationName = operationName;
	}

	public Span createSpan() {
		final Span span;
		if (!TracingUtils.getTraceContext().isEmpty()) {
			final Span currentSpan = TracingUtils.getTraceContext().getCurrentSpan();
			span = tracer.buildSpan(operationName)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
					.asChildOf(currentSpan)
					.start();
		} else {
			// client spans should not be root spans
			span = tracer.buildSpan(operationName)
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
