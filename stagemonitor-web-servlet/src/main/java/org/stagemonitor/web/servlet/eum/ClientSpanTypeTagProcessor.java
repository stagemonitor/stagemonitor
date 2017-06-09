package org.stagemonitor.web.servlet.eum;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Tracer;

public class ClientSpanTypeTagProcessor extends ClientSpanTagProcessor {

	private final String tagName;
	private final String requestParameterName;
	private final Map<String, String> clientSpanType;

	public ClientSpanTypeTagProcessor() {
		this.tagName = "type";
		this.requestParameterName = "ty";
		clientSpanType = new HashMap<String, String>();
		clientSpanType.put("pl", "client_pageload");
		clientSpanType.put("err", "client_error");
		clientSpanType.put("xhr", "client_ajax");
	}

	@Override
	protected void processSpanBuilderImpl(Tracer.SpanBuilder spanBuilder, Map<String, String[]> servletRequestParameters) {
		final String clientSubmittedType = getParameterValueOrNull(requestParameterName, servletRequestParameters);
		spanBuilder.withTag(tagName, clientSpanType.get(clientSubmittedType));
	}
}
