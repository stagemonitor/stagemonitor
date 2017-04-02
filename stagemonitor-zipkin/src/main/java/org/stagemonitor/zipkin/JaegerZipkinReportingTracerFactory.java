package org.stagemonitor.zipkin;

import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.CompositeReporter;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.senders.zipkin.ZipkinSender;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.TracerFactory;
import org.stagemonitor.requestmonitor.tracing.jaeger.LoggingSpanReporter;
import org.stagemonitor.requestmonitor.tracing.jaeger.Metric2RegistryStatsFactory;

import io.opentracing.Tracer;

public class JaegerZipkinReportingTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		return new com.uber.jaeger.Tracer.Builder(
				initArguments.getMeasurementSession().getApplicationName(),
				getReporter(initArguments),
				new ConstSampler(true))
				.build();
	}

	private Reporter getReporter(StagemonitorPlugin.InitArguments initArguments) {
		final ZipkinPlugin zipkinPlugin = initArguments.getPlugin(ZipkinPlugin.class);
		final LoggingSpanReporter loggingSpanReporter = new LoggingSpanReporter(initArguments.getPlugin(RequestMonitorPlugin.class));
		final Reporter zipkinReporter = getZipkinReporter(zipkinPlugin, initArguments.getMetricRegistry(), initArguments.getPlugin(CorePlugin.class));
		return new CompositeReporter(loggingSpanReporter, zipkinReporter);
	}

	private Reporter getZipkinReporter(ZipkinPlugin zipkinPlugin, Metric2Registry metricRegistry, CorePlugin corePlugin) {
		final Metrics metrics;
		if (corePlugin.isInternalMonitoringActive()) {
			metrics = new Metrics(new Metric2RegistryStatsFactory(metricRegistry));
		} else {
			metrics = new Metrics(new StatsFactoryImpl(new NullStatsReporter()));
		}
		return new RemoteReporter(ZipkinSender.create(zipkinPlugin.getZipkinEndpoint()),
				zipkinPlugin.getZipkinFlushInterval(), zipkinPlugin.getZipkinMaxQueueSize(), metrics);
	}
}
