package org.stagemonitor.web.servlet.eum;

import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;

public class ClientSpanStringTagProcessor extends ClientSpanTagProcessor {
	private final String tagName;
	private final String requestParameterName;
	private final boolean addTagsOnSpanBuilder;

	public ClientSpanStringTagProcessor(String type, String tagName, String requestParameterName) {
		this(type, tagName, requestParameterName, true);
	}

	public ClientSpanStringTagProcessor(String type, String tagName, String requestParameterName, boolean addTagsOnSpanBuilder) {
		super(type);
		this.tagName = tagName;
		this.requestParameterName = requestParameterName;
		this.addTagsOnSpanBuilder = addTagsOnSpanBuilder;
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		if (addTagsOnSpanBuilder) {
			final String valueOrNull = getParameterValueOrNull(requestParameterName, servletRequestParameters);
			spanBuilder.withTag(tagName, trimStringToMaxLength(valueOrNull));
		}
	}

	@Override
	protected void processSpanImpl(Span span, Map<String, String[]> servletRequestParameters) {
		if (!addTagsOnSpanBuilder) {
			final String valueOrNull = getParameterValueOrNull(requestParameterName, servletRequestParameters);
			span.setTag(tagName, trimStringToMaxLength(valueOrNull));
		}
	}
}
