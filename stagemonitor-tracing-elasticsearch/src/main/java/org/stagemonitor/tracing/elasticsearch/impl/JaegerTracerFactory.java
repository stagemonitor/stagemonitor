package org.stagemonitor.tracing.elasticsearch.impl;

import com.uber.jaeger.propagation.b3.B3TextMapCodec;
import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

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
		final B3TextMapCodec b3TextMapCodec = new B3TextMapCodec();
		final com.uber.jaeger.Tracer.Builder builder = new com.uber.jaeger.Tracer.Builder(
				initArguments.getMeasurementSession().getApplicationName(),
				new NoopReporter(),
				new ConstSampler(true))
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
		if (span instanceof com.uber.jaeger.Span) {
			final com.uber.jaeger.Span jaegerSpan = (com.uber.jaeger.Span) span;
			return jaegerSpan.context().getParentId() == 0;
		}
		return false;
	}

	@Override
	public boolean isSampled(Span span) {
		// TODO replace with Span#unwrap once https://github.com/opentracing/opentracing-java/pull/211 is merged
		if (span instanceof SpanWrapper) {
			span = ((SpanWrapper) span).getDelegate();
		}
		if (span instanceof com.uber.jaeger.Span) {
			final com.uber.jaeger.Span jaegerSpan = (com.uber.jaeger.Span) span;
			return jaegerSpan.context().isSampled();
		}
		return false;
	}
}
