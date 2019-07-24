package org.stagemonitor.tracing.soap;

import io.opentracing.Scope;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

public class TracingClientSOAPHandler extends AbstractTracingSOAPHandler {

	public TracingClientSOAPHandler() {
		super(false);
	}

	public TracingClientSOAPHandler(TracingPlugin tracingPlugin, SoapTracingPlugin soapTracingPlugin) {
		super(tracingPlugin, soapTracingPlugin, false);
	}

	@Override
	protected void handleOutboundSOAPMessage(SOAPMessageContext context) {
		final Tracer tracer = tracingPlugin.getTracer();
		final Tracer.SpanBuilder spanBuilder = tracer
				.buildSpan(getOperationName(context))
				.withTag(SpanUtils.OPERATION_TYPE, "soap")
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
		if (soapTracingPlugin.isSoapClientRecordRequestMessages()) {
			spanBuilder.withTag("soap.request", getSoapMessageAsString(context));
		}
		final Span span = spanBuilder.start();
		Scope scope = tracer.scopeManager().activate(span);
		currentScope.set(scope);
		tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new SOAPMessageInjectAdapter(context));
	}

	@Override
	protected void handleInboundSOAPMessage(SOAPMessageContext context) {
		if (soapTracingPlugin.isSoapClientRecordResponseMessages()) {
			final Span activeSpan = tracingPlugin.getTracer().scopeManager().activeSpan();
			if (activeSpan != null) {
				activeSpan.setTag("soap.response", getSoapMessageAsString(context));
			}
		}
	}

}
