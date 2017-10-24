package org.stagemonitor.tracing;

import org.stagemonitor.core.instrument.CallerUtil;
import org.stagemonitor.tracing.metrics.MetricsSpanEventListener;
import org.stagemonitor.tracing.utils.SpanUtils;

import io.opentracing.Scope;
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

	public Scope createScope() {
		final Tracer.SpanBuilder spanBuilder;
		if (tracer.scopeManager().active() != null) {
			spanBuilder = tracer.buildSpan(operationName)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
		} else {
			// client spans should not be root spans
			spanBuilder = tracer.buildSpan(operationName)
					.withTag(Tags.SAMPLING_PRIORITY.getKey(), 0)
					.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
		}
		if (trackMetricsPerOperationName()) {
			spanBuilder.withTag(MetricsSpanEventListener.ENABLE_TRACKING_METRICS_TAG, true);
		}
		spanBuilder.withTag(SpanUtils.OPERATION_TYPE, getType());
		return spanBuilder.startActive();
	}

	protected abstract String getType();

	protected boolean trackMetricsPerOperationName() {
		return false;
	}

	@Override
	public void execute() throws Exception {
	}
}
