package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.tracing.utils.SpanUtils;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public class ClientSpanTypeTagProcessor extends ClientSpanTagProcessor {

	private static final String JS_ERROR = "js_error";
	private static final String TYPE_PARAMETER_NAME = "ty";
	public static final String PAGELOAD = "pageload";
	private final Map<String, String> clientSpanType;

	public ClientSpanTypeTagProcessor() {
		clientSpanType = new HashMap<String, String>();
		clientSpanType.put("pl", PAGELOAD);
		clientSpanType.put("err", JS_ERROR);
		clientSpanType.put("xhr", "ajax");
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> requestParameters) {
		final String clientSubmittedType = getParameterValueOrNull(TYPE_PARAMETER_NAME, requestParameters);
		final String spanType = clientSpanType.get(clientSubmittedType);
		spanBuilder.withTag(SpanUtils.OPERATION_TYPE, spanType);
		if (JS_ERROR.equals(spanType)) {
			spanBuilder.withTag(Tags.ERROR.getKey(), true);
		}
	}
}
