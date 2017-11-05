package org.stagemonitor.zipkin;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracerFactory;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.wrapper.SpanWrapper;

import java.util.concurrent.TimeUnit;

import brave.Tracing;
import brave.opentracing.BraveSpan;
import brave.opentracing.BraveTracer;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import io.opentracing.Span;
import io.opentracing.Tracer;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.urlconnection.URLConnectionSender;

public class BraveZipkinReportingTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		final Tracing braveTracer = Tracing.newBuilder()
				.localServiceName(initArguments.getMeasurementSession().getApplicationName())
				.reporter(getZipkinReporterBuilder(initArguments).build())
				.sampler(getSampler())
				.build();
		return BraveTracer.newBuilder(braveTracer)
				.textMapPropagation(B3HeaderFormat.INSTANCE, Propagation.B3_STRING)
				.build();
	}

	@Override
	public boolean isRoot(Span span) {
		// TODO replace with Span#unwrap once https://github.com/opentracing/opentracing-java/pull/211 is merged
		if (span instanceof SpanWrapper) {
			span = ((SpanWrapper) span).getDelegate();
		}
		if (span instanceof BraveSpan) {
			final BraveSpan braveSpan = (BraveSpan) span;
			return braveSpan.unwrap().context().parentId() == null;
		}
		return false;
	}

	@Override
	public boolean isSampled(Span span) {
		// TODO replace with Span#unwrap once https://github.com/opentracing/opentracing-java/pull/211 is merged
		if (span instanceof SpanWrapper) {
			span = ((SpanWrapper) span).getDelegate();
		}
		if (span instanceof BraveSpan) {
			final BraveSpan braveSpan = (BraveSpan) span;
			return braveSpan.unwrap().context().sampled();
		}
		return false;
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
	 * See ConfigurationOptions in {@link TracingPlugin} tagged with sampling
	 */
	private static class AlwaysSampler extends Sampler {
		@Override
		public boolean isSampled(long traceId) {
			return true;
		}
	}

}
