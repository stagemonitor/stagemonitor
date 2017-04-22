package org.stagemonitor.zipkin;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.B3HeaderFormat;
import org.stagemonitor.requestmonitor.tracing.TracerFactory;

import java.util.concurrent.TimeUnit;

import brave.opentracing.BraveTracer;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import io.opentracing.Tracer;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.urlconnection.URLConnectionSender;

public class BraveZipkinReportingTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		final brave.Tracer braveTracer = brave.Tracer.newBuilder()
				.localServiceName(initArguments.getMeasurementSession().getApplicationName())
				.reporter(getZipkinReporterBuilder(initArguments).build())
				.sampler(getSampler())
				.build();
		return BraveTracer.newBuilder(braveTracer)
				.textMapPropagation(B3HeaderFormat.INSTANCE, Propagation.B3_STRING)
				.build();
	}

	protected AlwaysSampler getSampler() {
		return new AlwaysSampler();
	}

	protected AsyncReporter.Builder getZipkinReporterBuilder(StagemonitorPlugin.InitArguments initArguments) {
		final ZipkinPlugin zipkinPlugin = initArguments.getPlugin(ZipkinPlugin.class);
		final AsyncReporter.Builder reporterBuilder = AsyncReporter
				.builder(getSender(zipkinPlugin))
				.messageTimeout(zipkinPlugin.getZipkinFlushInterval(), TimeUnit.MILLISECONDS);

		final Integer zipkinMaxQueuedBytes = zipkinPlugin.getZipkinMaxQueuedBytes();
		if (zipkinMaxQueuedBytes != null) {
			reporterBuilder.queuedMaxBytes(zipkinMaxQueuedBytes);
		}
		if (initArguments.getPlugin(CorePlugin.class).isInternalMonitoringActive()) {
			reporterBuilder.metrics(new StagemonitorReporterMetrics(initArguments.getMetricRegistry()));
		}

		return reporterBuilder;
	}

	protected URLConnectionSender getSender(ZipkinPlugin zipkinPlugin) {
		return URLConnectionSender.create(zipkinPlugin.getZipkinEndpoint());
	}

	/**
	 * Sampling is performed by stagemonitor to ensure consistent configuration regardless of the OT impl
	 *
	 * See ConfigurationOptions in {@link RequestMonitorPlugin} tagged with sampling
	 */
	private static class AlwaysSampler extends Sampler {
		@Override
		public boolean isSampled(long traceId) {
			return true;
		}
	}

}
