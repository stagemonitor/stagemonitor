package org.stagemonitor.tracing.soap;

import io.opentracing.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(AbstractTracingSOAPHandler.class);

	private final boolean serverHandler;
	protected final TracingPlugin tracingPlugin;
	protected final SoapTracingPlugin soapTracingPlugin;

	protected static final ThreadLocal<Scope> currentScopeThreadLocal = new ThreadLocal<Scope>();

	public AbstractTracingSOAPHandler(boolean serverHandler) {
		this(Stagemonitor.getPlugin(TracingPlugin.class), Stagemonitor.getPlugin(SoapTracingPlugin.class), serverHandler);
	}

	protected AbstractTracingSOAPHandler(TracingPlugin tracingPlugin, SoapTracingPlugin soapTracingPlugin, boolean serverHandler) {
		this.tracingPlugin = tracingPlugin;
		this.soapTracingPlugin = soapTracingPlugin;
		this.serverHandler = serverHandler;
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
		if (!shouldExecute(context)) {
			return true;
		}
		if (isOutbound(context)) {
			handleOutboundSOAPMessage(context);
		} else {
			handleInboundSOAPMessage(context);
		}
		return true;
	}

	private boolean shouldExecute(MessageContext context) {
		if (serverHandler) {
			if (!isServerMessage(context)) {
				return false;
			}
		} else {
			if (isServerMessage(context)) {
				return false;
			}
		}
		return true;
	}

	protected void handleInboundSOAPMessage(SOAPMessageContext context) {
	}

	protected void handleOutboundSOAPMessage(SOAPMessageContext context) {
	}

	private boolean isServerMessage(MessageContext context) {
		return context.containsKey(MessageContext.SERVLET_REQUEST) ||
				context.containsKey(MessageContext.SERVLET_RESPONSE) ||
				context.containsKey(MessageContext.SERVLET_CONTEXT);
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		if (!shouldExecute(context)) {
			return true;
		}
		final Span activeSpan = tracingPlugin.getTracer().activeSpan();
		if (activeSpan != null) {
			Tags.ERROR.set(activeSpan, Boolean.TRUE);
			try {
				final SOAPFault fault = context.getMessage().getSOAPBody().getFault();
				activeSpan.setTag("soap.fault.reason", fault.getFaultString());
				activeSpan.setTag("soap.fault.code", fault.getFaultCode());
			} catch (SOAPException e) {
				logger.warn("Exception while trying to access SOAP fault (this exception was suppressed)", e);
			}
		}
		return true;
	}

	@Override
	public void close(MessageContext context) {
		if (!shouldExecute(context)) {
			return;
		}
		Span span = tracingPlugin.getTracer().activeSpan();
		if (span != null) {
			span.finish();
			Scope scope = currentScopeThreadLocal.get();
			currentScopeThreadLocal.remove();
			if (scope != null) {
				scope.close();
			}
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
