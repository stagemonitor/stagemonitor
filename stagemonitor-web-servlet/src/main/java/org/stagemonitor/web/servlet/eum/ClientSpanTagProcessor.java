package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

import static org.stagemonitor.web.servlet.eum.ClientSpanServlet.PARAMETER_TYPE;

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

	public void processSpanBuilder(Tracer.SpanBuilder spanBuilder, Map<String, String[]> requestParameters) {
		if (shouldProcess(requestParameters)) {
			processSpanBuilderImpl(spanBuilder, requestParameters);
		}
	}

	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> requestParameters) {
		// default no-op
	}

	protected boolean shouldProcess(Map<String, String[]> requestParams) {
		for (String requiredParam : requiredParams) {
			if (StringUtils.isEmpty(getParameterValueOrNull(requiredParam, requestParams))) {
				return false;
			}
		}
		return typeToProcess == null || typeToProcess.equals(getParameterValueOrNull(PARAMETER_TYPE, requestParams));
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

	protected void discardSpan(Span span) {
		Tags.SAMPLING_PRIORITY.set(span, 0);
	}

	protected Long parsedLongOrNull(String valueOrNull) {
		try {
			return Long.parseLong(valueOrNull);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
