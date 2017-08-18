package org.stagemonitor.tracing.soap;

import com.uber.jaeger.context.TracingUtils;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.TracingPlugin;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public abstract class AbstractTracingSOAPHandler implements SOAPHandler<SOAPMessageContext> {
	protected final TracingPlugin tracingPlugin;
	protected final SoapTracingPlugin soapTracingPlugin;

	public AbstractTracingSOAPHandler() {
		this(Stagemonitor.getPlugin(TracingPlugin.class), Stagemonitor.getPlugin(SoapTracingPlugin.class));
	}

	protected AbstractTracingSOAPHandler(TracingPlugin tracingPlugin, SoapTracingPlugin soapTracingPlugin) {
		this.tracingPlugin = tracingPlugin;
		this.soapTracingPlugin = soapTracingPlugin;
	}

	public static boolean isOutbound(MessageContext messageContext) {
		return (Boolean) messageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
	}

	@Override
	public Set<QName> getHeaders() {
		return Collections.emptySet();
	}

	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		if (isOutbound(context)) {
			handleOutboundSOAPMessage(context);
		} else {
			handleInboundSOAPMessage(context);
		}
		return true;
	}

	protected void handleInboundSOAPMessage(SOAPMessageContext context) {
	}

	protected void handleOutboundSOAPMessage(SOAPMessageContext context) {
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		if (!TracingUtils.getTraceContext().isEmpty()) {
			final Span span = TracingUtils.getTraceContext().getCurrentSpan();
			Tags.ERROR.set(span, Boolean.TRUE);
			try {
				final SOAPFault fault = context.getMessage().getSOAPBody().getFault();
				span.setTag("soap.fault.reason", fault.getFaultString());
				span.setTag("soap.fault.code", fault.getFaultCode());
			} catch (SOAPException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	public void close(MessageContext context) {
		if (!TracingUtils.getTraceContext().isEmpty()) {
			TracingUtils.getTraceContext().getCurrentSpan().finish();
		}
	}

	protected String getOperationName(SOAPMessageContext context) {
		return ((QName) context.get(MessageContext.WSDL_OPERATION)).getLocalPart();
	}

	public static String getSoapMessageAsString(SOAPMessageContext context) {
		try {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			context.getMessage().writeTo(os);
			os.close();
			return new String(os.toByteArray(), Charset.forName("UTF-8"));
		} catch (Exception e) {
			return null;
		}
	}
}
