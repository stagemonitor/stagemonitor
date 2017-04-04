package org.stagemonitor.zipkin;

import com.codahale.metrics.Gauge;

import org.stagemonitor.core.StagemonitorPlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.TracerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import brave.opentracing.BraveTracer;
import brave.sampler.Sampler;
import io.opentracing.Tracer;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.ReporterMetrics;
import zipkin.reporter.okhttp3.OkHttpSender;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class BraveZipkinReportingTracerFactory extends TracerFactory {

	@Override
	public Tracer getTracer(StagemonitorPlugin.InitArguments initArguments) {
		return BraveTracer.wrap(brave.Tracer.newBuilder()
				.localServiceName(initArguments.getMeasurementSession().getApplicationName())
				.reporter(getZipkinReporter(initArguments))
				.sampler(new AlwaysSampler())
				.build());
	}

	private AsyncReporter<Span> getZipkinReporter(StagemonitorPlugin.InitArguments initArguments) {
		final ZipkinPlugin zipkinPlugin = initArguments.getPlugin(ZipkinPlugin.class);
		return AsyncReporter
				.builder(OkHttpSender.create(zipkinPlugin.getZipkinEndpoint()))
				.queuedMaxBytes(zipkinPlugin.getZipkinMaxQueuedBytes())
				.messageTimeout(zipkinPlugin.getZipkinFlushInterval(), TimeUnit.MILLISECONDS)
				.metrics(new StagemonitorReporterMetrics(initArguments.getMetricRegistry()))
				.build();
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

	private static class StagemonitorReporterMetrics implements ReporterMetrics {
		private final Metric2Registry metricRegistry;

		private AtomicInteger queuedSpans = new AtomicInteger(0);
		private AtomicInteger queuedBytes = new AtomicInteger(0);

		public StagemonitorReporterMetrics(Metric2Registry metricRegistry) {
			this.metricRegistry = metricRegistry;
			metricRegistry.register(name("spans_queued").build(), new Gauge<Integer>() {
				@Override
				public Integer getValue() {
					return queuedSpans.get();
				}
			});
			metricRegistry.register(name("spans_queued_bytes").build(), new Gauge<Integer>() {
				@Override
				public Integer getValue() {
					return queuedBytes.get();
				}
			});
		}

		@Override
		public void incrementMessages() {
			metricRegistry.counter(name("messages_sent").build()).inc();
		}

		@Override
		public void incrementMessageBytes(int quantity) {
			metricRegistry.counter(name("messages_sent_bytes").build()).inc(quantity);
		}

		@Override
		public void incrementMessagesDropped(Throwable cause) {
			metricRegistry.counter(name("messages_dropped").build()).inc();
		}

		@Override
		public void incrementSpans(int quantity) {
			metricRegistry.counter(name("spans_reported").build()).inc();
		}

		@Override
		public void incrementSpanBytes(int quantity) {
			metricRegistry.counter(name("spans_reported_bytes").build()).inc();
		}

		@Override
		public void incrementSpansDropped(int quantity) {
			metricRegistry.counter(name("spans_dropped").build()).inc(quantity);
		}

		@Override
		public void updateQueuedSpans(int update) {
			queuedSpans.set(update);
		}

		@Override
		public void updateQueuedBytes(int update) {
			queuedBytes.set(update);
		}
	}
}
