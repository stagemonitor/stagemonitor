package org.stagemonitor.web.servlet.eum;

import java.util.Collections;
import java.util.Map;

import io.opentracing.Tracer;

class ClientSpanLongTagProcessor extends ClientSpanTagProcessor {

	private final String tagName;
	private final String requestParameterName;

	ClientSpanLongTagProcessor(String type, String tagName, String requestParameterName) {
		super(type, Collections.singletonList(requestParameterName));
		this.tagName = tagName;
		this.requestParameterName = requestParameterName;
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		final String valueOrNull = getParameterValueOrNull(requestParameterName, servletRequestParameters);
		spanBuilder.withTag(tagName, parsedLongOrNull(valueOrNull));
	}

	private Long parsedLongOrNull(String valueOrNull) {
		Long parsedValue;
		try {
			parsedValue = Long.parseLong(valueOrNull);
		} catch (NumberFormatException e) {
			parsedValue = null;
		}
		return parsedValue;
	}
}
