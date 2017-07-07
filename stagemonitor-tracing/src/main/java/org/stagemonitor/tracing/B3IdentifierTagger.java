package org.stagemonitor.tracing;

import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;

class B3IdentifierTagger extends StatelessSpanEventListener {
	private final TracingPlugin tracingPlugin;

	B3IdentifierTagger(TracingPlugin tracingPlugin) {
		this.tracingPlugin = tracingPlugin;
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		final B3HeaderFormat.B3Identifiers b3Identifiers = B3HeaderFormat.getB3Identifiers(tracingPlugin.getTracer(), spanWrapper);
		spanWrapper.setTag("id", b3Identifiers.getSpanId());
		spanWrapper.setTag("trace_id", b3Identifiers.getTraceId());
		spanWrapper.setTag("parent_id", b3Identifiers.getParentSpanId());
	}
}
