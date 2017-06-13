package org.stagemonitor.web.servlet.eum;

import java.util.Map;

import io.opentracing.Tracer;

public class ClientSpanStringTagProcessor extends ClientSpanTagProcessor {
	private final String tagName;
	private final String requestParameterName;

	ClientSpanStringTagProcessor(String type, String tagName, String requestParameterName) {
		super(type);
		this.tagName = tagName;
		this.requestParameterName = requestParameterName;
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		final String valueOrNull = getParameterValueOrNull(requestParameterName, servletRequestParameters);
		spanBuilder.withTag(tagName, trimStringToMaxLength(valueOrNull));
	}
}
