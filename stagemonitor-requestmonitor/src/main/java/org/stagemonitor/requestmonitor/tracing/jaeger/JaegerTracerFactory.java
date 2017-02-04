package org.stagemonitor.requestmonitor.tracing.jaeger;

import com.uber.jaeger.reporters.CompositeReporter;
import com.uber.jaeger.reporters.LoggingReporter;
import com.uber.jaeger.samplers.ConstSampler;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.TracerFactory;

import io.opentracing.Tracer;

public class JaegerTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		return new com.uber.jaeger.Tracer.Builder(
				initArguments.getMeasurementSession().getApplicationName(),
				new CompositeReporter(new LoggingReporter()),
				new ConstSampler(true))
				.build();
	}
}
