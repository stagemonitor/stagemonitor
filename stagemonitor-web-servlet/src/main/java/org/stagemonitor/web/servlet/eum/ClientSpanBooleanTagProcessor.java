package org.stagemonitor.web.servlet.eum;

import java.util.Map;

import io.opentracing.Tracer;

public class ClientSpanBooleanTagProcessor extends ClientSpanTagProcessor {

	private final String tagName;
	private final String requestParameterName;

	ClientSpanBooleanTagProcessor(String type, String tagName, String requestParameterName) {
		super(type);
		this.tagName = tagName;
		this.requestParameterName = requestParameterName;
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> requestParameters) {
		final String valueOrNull = getParameterValueOrNull(requestParameterName, requestParameters);
		spanBuilder.withTag(tagName, parseBooleanOrFalse(valueOrNull));
	}

	public static Boolean parseBooleanOrFalse(String valueOrNull) {
		return "1".equals(valueOrNull) || Boolean.parseBoolean(valueOrNull);
	}

}
