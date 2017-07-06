package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.tracing.utils.SpanUtils;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public class ClientSpanTypeTagProcessor extends ClientSpanTagProcessor {

	private static final String JS_ERROR = "js_error";
	private final String tagName;
	private final String requestParameterName;
	private final Map<String, String> clientSpanType;

	public ClientSpanTypeTagProcessor() {
		this.tagName = SpanUtils.OPERATION_TYPE;
		this.requestParameterName = "ty";
		clientSpanType = new HashMap<String, String>();
		clientSpanType.put("pl", "pageload");
		clientSpanType.put("err", JS_ERROR);
		clientSpanType.put("xhr", "ajax");
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		final String clientSubmittedType = getParameterValueOrNull(requestParameterName, servletRequestParameters);
		final String spanType = clientSpanType.get(clientSubmittedType);
		spanBuilder.withTag(tagName, spanType);
		if (JS_ERROR.equals(spanType)) {
			spanBuilder.withTag(Tags.ERROR.getKey(), true);
		}
	}
}
