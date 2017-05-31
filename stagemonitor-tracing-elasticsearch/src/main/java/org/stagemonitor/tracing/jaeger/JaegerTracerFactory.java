package org.stagemonitor.tracing.jaeger;

import com.uber.jaeger.propagation.b3.B3TextMapCodec;
import com.uber.jaeger.reporters.NoopReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracerFactory;

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
}
