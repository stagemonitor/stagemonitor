package org.stagemonitor.requestmonitor.tracing.jaeger;

import com.uber.jaeger.samplers.ConstSampler;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.TracerFactory;

import io.opentracing.Tracer;

public class JaegerTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		return new com.uber.jaeger.Tracer.Builder(
				initArguments.getMeasurementSession().getApplicationName(),
				new LoggingSpanReporter(initArguments.getPlugin(RequestMonitorPlugin.class)),
				new ConstSampler(true))
				.build();
	}
}
