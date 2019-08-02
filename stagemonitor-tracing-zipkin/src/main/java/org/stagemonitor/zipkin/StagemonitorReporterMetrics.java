package org.stagemonitor.zipkin;

import com.codahale.metrics.Gauge;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

import java.util.concurrent.atomic.AtomicInteger;

import zipkin2.reporter.ReporterMetrics;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class StagemonitorReporterMetrics implements ReporterMetrics {
	private final Metric2Registry metricRegistry;

	private AtomicInteger queuedSpans = new AtomicInteger(0);
	private AtomicInteger queuedBytes = new AtomicInteger(0);

	StagemonitorReporterMetrics(Metric2Registry metricRegistry) {
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
