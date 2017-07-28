package org.stagemonitor.tracing.soap;

import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public class TracingServerSOAPHandler extends AbstractTracingSOAPHandler {

	public TracingServerSOAPHandler() {
	}

	public TracingServerSOAPHandler(TracingPlugin tracingPlugin, SoapTracingPlugin soapTracingPlugin) {
		super(tracingPlugin, soapTracingPlugin);
	}

	@Override
	protected void handleInboundSOAPMessage(SOAPMessageContext context) {
		final Tracer.SpanBuilder spanBuilder = tracingPlugin.getTracer()
				.buildSpan(getOperationName(context))
				.withTag(SpanUtils.OPERATION_TYPE, "soap")
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
		if (soapTracingPlugin.isSoapServerRecordRequestMessages()) {
			spanBuilder.withTag("soap.request", getSoapMessageAsString(context));
		}
		if (!TracingUtils.getTraceContext().isEmpty()) {
			spanBuilder.asChildOf(TracingUtils.getTraceContext().getCurrentSpan());
		}
		spanBuilder.start();
	}

	@Override
	protected void handleOutboundSOAPMessage(SOAPMessageContext context) {
		if (soapTracingPlugin.isSoapServerRecordResponseMessages()) {
			if (!TracingUtils.getTraceContext().isEmpty()) {
				final Span span = TracingUtils.getTraceContext().getCurrentSpan();
				span.setTag("soap.response", getSoapMessageAsString(context));
			}
		}
	}

}
