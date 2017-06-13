package org.stagemonitor.web.servlet.eum;

import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;

abstract class ClientSpanTagProcessor {

	private static final int MAX_LENGTH = 255;
	private final String typeToProcess;

	protected ClientSpanTagProcessor(String weaselOperationTypeToProcess) {
		this.typeToProcess = weaselOperationTypeToProcess;
	}

	protected ClientSpanTagProcessor() {
		typeToProcess = null; // process all types
	}

	public void processSpanBuilder(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		if (shouldProcess(servletRequestParameters)) {
			processSpanBuilderImpl(spanBuilder, servletRequestParameters);
		}
	}

	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		// default no-op
	}

	private boolean shouldProcess(Map<String, String[]> servletRequestParameters) {
		final String type = getParameterValueOrNull(ClientSpanServlet.PARAMETER_TYPE, servletRequestParameters);
		return typeToProcess == null || typeToProcess.equals(type);
	}

	String getParameterValueOrNull(String key, Map<String, String[]> servletRequestParameters) {
		if (servletRequestParameters != null
				&& servletRequestParameters.containsKey(key)
				&& servletRequestParameters.get(key).length > 0) {
			return servletRequestParameters.get(key)[0];
		} else {
			return null;
		}
	}

	public void processSpan(Span span, Map<String, String[]> servletRequestParameters) {
		if (shouldProcess(servletRequestParameters)) {
			processSpanImpl(span, servletRequestParameters);
		}
	}

	protected void processSpanImpl(Span span, Map<String, String[]> servletRequestParameters) {
		// default no-op
	}

	protected String trimStringToMaxLength(String string) {
		if (string == null || string.length() <= MAX_LENGTH) {
			return string;
		} else {
			return string.substring(0, MAX_LENGTH);
		}
	}


}
