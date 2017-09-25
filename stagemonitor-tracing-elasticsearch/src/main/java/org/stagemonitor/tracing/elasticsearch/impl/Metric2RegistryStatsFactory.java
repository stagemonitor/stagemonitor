package org.stagemonitor.tracing.elasticsearch.impl;

import com.uber.jaeger.metrics.Counter;
import com.uber.jaeger.metrics.Gauge;
import com.uber.jaeger.metrics.StatsFactory;
import com.uber.jaeger.metrics.Timer;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class Metric2RegistryStatsFactory implements StatsFactory {

	private final Metric2Registry metric2Registry;

	public Metric2RegistryStatsFactory(Metric2Registry metric2Registry) {
		this.metric2Registry = metric2Registry;
	}

	@Override
	public Counter createCounter(String name, Map<String, String> tags) {
		final com.codahale.metrics.Counter counter = metric2Registry.counter(name(name).tags(tags).build());
		return new Counter() {
			@Override
			public void inc(long delta) {
				counter.inc(delta);
			}
		};
	}

	@Override
	public Timer createTimer(String name, Map<String, String> tags) {
		final com.codahale.metrics.Timer timer = metric2Registry.timer(name(name).tags(tags).build());
		return new Timer() {
			@Override
			public void durationMicros(long time) {
				timer.update(time, TimeUnit.MICROSECONDS);
			}
		};
	}

	@Override
	public Gauge createGauge(String name, Map<String, String> tags) {
		final AtomicLong value = new AtomicLong(0);
		final com.codahale.metrics.Gauge<Long> gauge = new com.codahale.metrics.Gauge<Long>() {
			@Override
			public Long getValue() {
				return value.longValue();
			}
		};
		metric2Registry.register(name(name).tags(tags).build(), gauge);
		return new Gauge() {
			@Override
			public void update(long amount) {
				value.set(amount);
			}
		};
	}
}
