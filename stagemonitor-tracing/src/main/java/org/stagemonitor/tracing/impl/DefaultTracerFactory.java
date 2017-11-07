package org.stagemonitor.tracing.impl;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracerFactory;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

public class DefaultTracerFactory extends TracerFactory {
	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		return new DefaultTracerImpl();
	}

	@Override
	public boolean isRoot(Span span) {
		final SpanContextInformation contextInformation = SpanContextInformation.get(span);
		if (contextInformation != null) {
			return contextInformation.getParent() == null;
		}
		return true;
	}

	@Override
	public boolean isSampled(Span span) {
		if (span instanceof SpanWrapper) {
			final Number samplingPrio = ((SpanWrapper) span).getNumberTag(Tags.SAMPLING_PRIORITY.getKey());
			return !Integer.valueOf(0).equals(samplingPrio);
		}
		return false;
	}
}
