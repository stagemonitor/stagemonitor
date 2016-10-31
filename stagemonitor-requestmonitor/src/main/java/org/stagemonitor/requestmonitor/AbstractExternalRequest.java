package org.stagemonitor.requestmonitor;

import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.instrument.CallerUtil;
import org.stagemonitor.requestmonitor.utils.Spans;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public abstract class AbstractExternalRequest implements MonitoredRequest<RequestTrace> {

	private final RequestMonitorPlugin requestMonitorPlugin;

	protected AbstractExternalRequest(RequestMonitorPlugin requestMonitorPlugin) {
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	public String getInstanceName() {
		return null;
	}

	public RequestTrace createRequestTrace() {
		return null;
	}

	public Span createSpan() {
		final Tracer tracer = requestMonitorPlugin.getTracer();
		final String callerSignature = CallerUtil.getCallerSignature();
		final Span span;
		if (!TracingUtils.getTraceContext().isEmpty()) {
			final Span currentSpan = TracingUtils.getTraceContext().getCurrentSpan();
			span = tracer.buildSpan(callerSignature).asChildOf(currentSpan).start();
			span.setTag("parent_name", ((com.uber.jaeger.Span)span).getOperationName());
		} else {
			span = tracer.buildSpan(callerSignature).start();
		}
		Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
		Spans.setOperationType(span, getType());
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

	@Override
	public void onPostExecute(RequestMonitor.RequestInformation<RequestTrace> requestInformation) {
	}
}
