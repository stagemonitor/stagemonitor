package org.stagemonitor.requestmonitor.metrics;

import com.codahale.metrics.Timer;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.core.util.TimeUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.tracing.wrapper.ClientServerAwareSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;

import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ServerRequestMetricsSpanEventListener extends ClientServerAwareSpanEventListener {

	private static final MetricName.MetricNameTemplate responseTimeCpuTemplate = name("response_time_cpu")
			.tag("request_name", "").layer("All")
			.templateFor("request_name");
	private static final MetricName.MetricNameTemplate errorRateTemplate = name("error_rate_server")
			.tag("request_name", "")
			.layer("All")
			.templateFor("request_name");
	private static MetricName.MetricNameTemplate timerMetricNameTemplate = name("response_time_server")
			.tag("request_name", "")
			.layer("All")
			.templateFor("request_name");

	private static final MetricName.MetricNameTemplate externalRequestRateTemplate = name("external_requests_rate")
			.templateFor("request_name", "type");
	private static final MetricName.MetricNameTemplate responseTimeExternalRequestLayerTemplate = name("response_time_server")
			.templateFor("request_name", "layer");

	private final Metric2Registry metricRegistry;
	private final RequestMonitorPlugin requestMonitorPlugin;

	private long startCpu;
	private boolean error;

	public ServerRequestMetricsSpanEventListener(Metric2Registry metricRegistry, RequestMonitorPlugin requestMonitorPlugin) {
		this.metricRegistry = metricRegistry;
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	@Override
	public void onStart(SpanWrapper spanWrapper) {
		startCpu = TimeUtils.getCpuTime();
	}

	@Override
	public boolean onSetTag(String key, boolean value) {
		if (Tags.ERROR.getKey().equals(key)) {
			error = value;
		}
		return value;
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation contextInformation = SpanContextInformation.forSpan(spanWrapper);
		if (isServer && StringUtils.isNotEmpty(operationName)) {
			final long cpuTime = trackCpuTime(spanWrapper.getDelegate());
			trackMetrics(spanWrapper, operationName, durationNanos, cpuTime);
			trackExternalRequestMetricsOfParent(spanWrapper.getDelegate(), operationName, contextInformation);
		}
	}

	private long trackCpuTime(Span span) {
		final long cpuTime = TimeUtils.getCpuTime() - startCpu;
		span.setTag("duration_cpu", NANOSECONDS.toMicros(cpuTime));
		span.setTag("duration_cpu_ms", NANOSECONDS.toMillis(cpuTime));
		return cpuTime;
	}

	private void trackMetrics(Span span, String operationName, long durationNanos, long cpuTime) {
		final Timer timer = metricRegistry.timer(getTimerMetricName(operationName));
		timer.update(durationNanos, NANOSECONDS);
		final SpanContextInformation spanContext = SpanContextInformation.forSpan(span);
		spanContext.setTimerForThisRequest(timer);

		metricRegistry.timer(getTimerMetricName("All")).update(durationNanos, NANOSECONDS);

		if (requestMonitorPlugin.isCollectCpuTime()) {
			metricRegistry.timer(responseTimeCpuTemplate.build(operationName)).update(cpuTime, MICROSECONDS);
			metricRegistry.timer(responseTimeCpuTemplate.build("All")).update(cpuTime, MICROSECONDS);
		}

		if (error) {
			metricRegistry.meter(getErrorMetricName(operationName)).mark();
			metricRegistry.meter(getErrorMetricName("All")).mark();
		}
	}

	/*
	 * tracks the external requests grouped by the parent request name
	 */
	private void trackExternalRequestMetricsOfParent(Span span, String operationName, SpanContextInformation spanContext) {
		for (SpanContextInformation.ExternalRequestStats externalRequestStats : spanContext.getExternalRequestStats()) {
			long durationNanos = externalRequestStats.getExecutionTimeNanos();
			final String requestType = externalRequestStats.getRequestType();
			span.setTag("external_requests." + requestType + ".duration_ms", TimeUnit.NANOSECONDS.toMillis(durationNanos));
			span.setTag("external_requests." + requestType + ".count", externalRequestStats.getExecutionCount());

			if (durationNanos > 0) {
				if (requestMonitorPlugin.isCollectDbTimePerRequest()) {
					metricRegistry.timer(responseTimeExternalRequestLayerTemplate
							.build(operationName, requestType))
							.update(durationNanos, NANOSECONDS);
				}
				metricRegistry.timer(responseTimeExternalRequestLayerTemplate
						.build("All", requestType))
						.update(durationNanos, NANOSECONDS);
			}

			metricRegistry.meter(externalRequestRateTemplate
					.build(operationName, requestType))
					.mark(externalRequestStats.getExecutionCount());
		}
	}

	public static MetricName getErrorMetricName(String requestName) {
		return errorRateTemplate.build(requestName);
	}

	public static MetricName getTimerMetricName(String requestName) {
		return timerMetricNameTemplate.build(requestName);
	}

	public static class Factory implements SpanEventListenerFactory {
		private final Metric2Registry metricRegistry;
		private final RequestMonitorPlugin requestMonitorPlugin;

		public Factory(Metric2Registry metricRegistry, RequestMonitorPlugin requestMonitorPlugin) {
			this.metricRegistry = metricRegistry;
			this.requestMonitorPlugin = requestMonitorPlugin;
		}

		@Override
		public SpanEventListener create() {
			return new ServerRequestMetricsSpanEventListener(metricRegistry, requestMonitorPlugin);
		}
	}
}
