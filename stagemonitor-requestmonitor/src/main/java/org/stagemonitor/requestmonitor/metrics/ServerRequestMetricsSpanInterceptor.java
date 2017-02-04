package org.stagemonitor.requestmonitor.metrics;

import com.codahale.metrics.Timer;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.core.util.TimeUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.ClientServerAwareSpanInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;

import java.util.concurrent.Callable;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ServerRequestMetricsSpanInterceptor extends ClientServerAwareSpanInterceptor {

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

	public ServerRequestMetricsSpanInterceptor(Metric2Registry metricRegistry, RequestMonitorPlugin requestMonitorPlugin) {
		this.metricRegistry = metricRegistry;
		this.requestMonitorPlugin = requestMonitorPlugin;
	}


	public static Callable<SpanInterceptor> asCallable(final Metric2Registry metricRegistry, final RequestMonitorPlugin requestMonitorPlugin) {
		return new Callable<SpanInterceptor>() {
			@Override
			public SpanInterceptor call() throws Exception {
				return new ServerRequestMetricsSpanInterceptor(metricRegistry, requestMonitorPlugin);
			}
		};
	}

	@Override
	public void onStart() {
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
	public void onFinish(Span span, String operationName, long durationNanos) {
		if (isServer && StringUtils.isNotEmpty(operationName)) {
			final long cpuTime = trackCpuTime(span);
			trackMetrics(operationName, durationNanos, cpuTime);
		}
	}

	private long trackCpuTime(Span span) {
		final long cpuTime = TimeUtils.getCpuTime() - startCpu;
		span.setTag("duration_cpu", NANOSECONDS.toMicros(cpuTime));
		span.setTag("duration_cpu_ms", NANOSECONDS.toMillis(cpuTime));
		return cpuTime;
	}

	private void trackMetrics(String operationName, long durationNanos, long cpuTime) {
		final Timer timer = metricRegistry.timer(getTimerMetricName(operationName));
		timer.update(durationNanos, NANOSECONDS);
		final RequestMonitor.RequestInformation requestInformation = requestMonitorPlugin.getRequestMonitor().getRequestInformation();
		requestInformation.setTimerForThisRequest(timer);

		metricRegistry.timer(getTimerMetricName("All")).update(durationNanos, NANOSECONDS);

		if (requestMonitorPlugin.isCollectCpuTime()) {
			metricRegistry.timer(responseTimeCpuTemplate.build(operationName)).update(cpuTime, MICROSECONDS);
			metricRegistry.timer(responseTimeCpuTemplate.build("All")).update(cpuTime, MICROSECONDS);
		}

		if (error) {
			metricRegistry.meter(getErrorMetricName(operationName)).mark();
			metricRegistry.meter(getErrorMetricName("All")).mark();
		}
		trackExternalRequestMetricsOfParent(operationName, requestInformation);
	}

	/*
	 * tracks the external requests grouped by the parent request name
	 */
	private void trackExternalRequestMetricsOfParent(String operationName, RequestMonitor.RequestInformation requestInformation) {
		for (RequestMonitor.RequestInformation.ExternalRequestStats externalRequestStats : requestInformation.getExternalRequestStats()) {
			long durationNanos = externalRequestStats.getExecutionTimeNanos();

			if (durationNanos > 0) {
				if (requestMonitorPlugin.isCollectDbTimePerRequest()) {
					metricRegistry.timer(responseTimeExternalRequestLayerTemplate
							.build(operationName, externalRequestStats.getRequestType()))
							.update(durationNanos, NANOSECONDS);
				}
				metricRegistry.timer(responseTimeExternalRequestLayerTemplate
						.build("All", externalRequestStats.getRequestType()))
						.update(durationNanos, NANOSECONDS);
			}

			metricRegistry.meter(externalRequestRateTemplate
					.build(operationName, externalRequestStats.getRequestType()))
					.mark();
		}
	}

	public static MetricName getErrorMetricName(String requestName) {
		return errorRateTemplate.build(requestName);
	}

	public static MetricName getTimerMetricName(String requestName) {
		return timerMetricNameTemplate.build(requestName);
	}
}
