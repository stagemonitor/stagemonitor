package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;

abstract class ClientSpanTagProcessor {

	static final int MAX_LENGTH = 255;
	private final String typeToProcess;
	private final Collection<String> requiredParams;

	protected ClientSpanTagProcessor() {
		this(null);
	}

	protected ClientSpanTagProcessor(String weaselOperationTypeToProcess) {
		this(weaselOperationTypeToProcess, Collections.<String>emptyList());
	}

	protected ClientSpanTagProcessor(String weaselOperationTypeToProcess, Collection<String> requiredParams) {
		this.typeToProcess = weaselOperationTypeToProcess;
		this.requiredParams = requiredParams;
	}

	public void processSpanBuilder(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		if (shouldProcess(servletRequestParameters)) {
			processSpanBuilderImpl(spanBuilder, servletRequestParameters);
		}
	}

	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		// default no-op
	}

	protected boolean shouldProcess(Map<String, String[]> servletRequestParameters) {
		for (String requiredParam : requiredParams) {
			if (StringUtils.isEmpty(getParameterValueOrNull(requiredParam, servletRequestParameters))) {
				return false;
			}
		}
		final String type = getParameterValueOrNull(ClientSpanServlet.PARAMETER_TYPE, servletRequestParameters);
		return typeToProcess != null && typeToProcess.equals(type);
	}

	public final String getParameterValueOrNull(String key, Map<String, String[]> servletRequestParameters) {
		if (servletRequestParameters != null
				&& servletRequestParameters.containsKey(key)
				&& servletRequestParameters.get(key).length > 0) {
			return servletRequestParameters.get(key)[0];
		} else {
			return null;
		}
	}

	public final void processSpan(Span span, Map<String, String[]> servletRequestParameters) {
		if (shouldProcess(servletRequestParameters)) {
			processSpanImpl(span, servletRequestParameters);
		}
	}

	protected void processSpanImpl(Span span, Map<String, String[]> servletRequestParameters) {
		// default no-op
	}

	protected final String trimStringToMaxLength(String string) {
		return trimStringToLength(string, MAX_LENGTH);
	}

	protected final String trimStringToLength(String string, int length) {
		if (string == null || string.length() <= length) {
			return string;
		} else {
			return string.substring(0, length);
		}
	}


}
