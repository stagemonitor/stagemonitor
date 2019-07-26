package org.stagemonitor.tracing.elasticsearch.impl;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.propagation.B3TextMapCodec;
import io.jaegertracing.internal.reporters.NoopReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracerFactory;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public class JaegerTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		final B3TextMapCodec b3TextMapCodec = new B3TextMapCodec.Builder().build();
		final JaegerTracer.Builder builder = new JaegerTracer.Builder(
				initArguments.getMeasurementSession().getApplicationName())
				.withReporter(new NoopReporter())
				.withSampler(new ConstSampler(true))
				.registerInjector(B3HeaderFormat.INSTANCE, b3TextMapCodec)
				.registerInjector(Format.Builtin.HTTP_HEADERS, b3TextMapCodec)
				.registerExtractor(Format.Builtin.HTTP_HEADERS, b3TextMapCodec);
		return builder.build();
	}

	@Override
	public boolean isRoot(Span span) {
		// TODO replace with Span#unwrap once https://github.com/opentracing/opentracing-java/pull/211 is merged
		if (span instanceof SpanWrapper) {
			span = ((SpanWrapper) span).getDelegate();
		}
		if (span instanceof io.jaegertracing.internal.JaegerSpan) {
			final io.jaegertracing.internal.JaegerSpan jaegerSpan = (io.jaegertracing.internal.JaegerSpan) span;
			return jaegerSpan.context().getParentId() == 0;
		}
		return false;
	}

	@Override
	public boolean isSampled(Span span) {
		if (span instanceof SpanWrapper) {
			span = ((SpanWrapper) span).unwrap(Span.class);
		}

		if (span instanceof io.jaegertracing.internal.JaegerSpan) {
			final io.jaegertracing.internal.JaegerSpan jaegerSpan = (io.jaegertracing.internal.JaegerSpan) span;
			return jaegerSpan.context().isSampled();
		}
		return false;
	}
}
