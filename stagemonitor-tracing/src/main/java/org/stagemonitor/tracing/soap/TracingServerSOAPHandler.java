package org.stagemonitor.tracing.soap;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;

import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 * This class will be injected to implementations of {@link javax.xml.ws.Binding}
 *
 * <p> In JBoss for example this implementations is loaded by a module class loader which means we have one class which
 * is accessed by multiple Applications </p>
 */
public class TracingServerSOAPHandler extends AbstractTracingSOAPHandler {

	public TracingServerSOAPHandler() {
		super(true);
	}

	public TracingServerSOAPHandler(TracingPlugin tracingPlugin, SoapTracingPlugin soapTracingPlugin) {
		super(tracingPlugin, soapTracingPlugin, true);
	}

	@Override
	protected void handleInboundSOAPMessage(SOAPMessageContext context) {
		final Tracer tracer = tracingPlugin.getTracer();
		final Tracer.SpanBuilder spanBuilder = tracer
				.buildSpan(getOperationName(context))
				.withTag(SpanUtils.OPERATION_TYPE, "soap")
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
		if (soapTracingPlugin.isSoapServerRecordRequestMessages()) {
			spanBuilder.withTag("soap.request", getSoapMessageAsString(context));
		}
		final Scope scope = tracer.activateSpan(spanBuilder.start());
		currentScopeThreadLocal.set(scope);
	}

	@Override
	protected void handleOutboundSOAPMessage(SOAPMessageContext context) {
		if (soapTracingPlugin.isSoapServerRecordResponseMessages()) {
			final Span activeSpan = tracingPlugin.getTracer().activeSpan();
			if (activeSpan != null) {
				activeSpan.setTag("soap.response", getSoapMessageAsString(context));
			}
		}
	}

}
