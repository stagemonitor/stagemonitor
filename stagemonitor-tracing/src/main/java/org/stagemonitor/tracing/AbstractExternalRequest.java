package org.stagemonitor.tracing;

import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.instrument.CallerUtil;
import org.stagemonitor.tracing.utils.SpanUtils;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public abstract class AbstractExternalRequest extends MonitoredRequest {

	public static final String EXTERNAL_REQUEST_METHOD = "method";
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
		final Tracer.SpanBuilder spanBuilder;
		if (!TracingUtils.getTraceContext().isEmpty()) {
			final Span currentSpan = TracingUtils.getTraceContext().getCurrentSpan();
			spanBuilder = tracer.buildSpan(operationName)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
					.asChildOf(currentSpan);
		} else {
			// client spans should not be root spans
			spanBuilder = tracer.buildSpan(operationName)
					.withTag(Tags.SAMPLING_PRIORITY.getKey(), 0)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
		}
		spanBuilder.withTag(SpanUtils.OPERATION_TYPE, getType());
		return spanBuilder.start();
	}

	protected abstract String getType();

	@Override
	public void execute() throws Exception {
	}
}
