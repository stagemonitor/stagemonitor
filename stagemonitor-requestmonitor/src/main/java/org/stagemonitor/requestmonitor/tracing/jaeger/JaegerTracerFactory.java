package org.stagemonitor.requestmonitor.tracing.jaeger;

import com.uber.jaeger.propagation.b3.B3TextMapCodec;
import com.uber.jaeger.samplers.ConstSampler;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.TracerFactory;

import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public class JaegerTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		final B3TextMapCodec b3TextMapCodec = new B3TextMapCodec();
		return new com.uber.jaeger.Tracer.Builder(
				initArguments.getMeasurementSession().getApplicationName(),
				new LoggingSpanReporter(initArguments.getPlugin(RequestMonitorPlugin.class)),
				new ConstSampler(true))
				.registerInjector(Format.Builtin.HTTP_HEADERS, b3TextMapCodec)
				.registerExtractor(Format.Builtin.HTTP_HEADERS, b3TextMapCodec)
				.build();
	}
}
