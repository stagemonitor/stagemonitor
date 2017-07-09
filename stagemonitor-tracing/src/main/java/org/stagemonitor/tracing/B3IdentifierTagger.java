package org.stagemonitor.tracing;

import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

public class B3IdentifierTagger extends StatelessSpanEventListener {
	public static final String SPAN_ID = "id";
	public static final String TRACE_ID = "trace_id";
	public static final String PARENT_ID = "parent_id";

	private final TracingPlugin tracingPlugin;

	B3IdentifierTagger(TracingPlugin tracingPlugin) {
		this.tracingPlugin = tracingPlugin;
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		final B3HeaderFormat.B3Identifiers b3Identifiers = B3HeaderFormat.getB3Identifiers(tracingPlugin.getTracer(), spanWrapper);
		spanWrapper.setTag(SPAN_ID, b3Identifiers.getSpanId());
		spanWrapper.setTag(TRACE_ID, b3Identifiers.getTraceId());
		spanWrapper.setTag(PARENT_ID, b3Identifiers.getParentSpanId());
	}
}
