package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.ClientServerAwareSpanInterceptor;

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
	private static final String DURATION_CPU = "duration_cpu";
	private static MetricName.MetricNameTemplate timerMetricNameTemplate = name("response_time_server")
			.tag("request_name", "")
			.layer("All")
			.templateFor("request_name");

	private final Metric2Registry metricRegistry;
	private final RequestMonitorPlugin requestMonitorPlugin;

	private Number durationCpu;
	private boolean error;

	public ServerRequestMetricsSpanInterceptor(Metric2Registry metricRegistry, RequestMonitorPlugin requestMonitorPlugin) {
		this.metricRegistry = metricRegistry;
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	@Override
	public Number onSetTag(String key, Number value) {
		if (DURATION_CPU.equals(key)) {
			durationCpu = value;
		}
		return value;
	}

	@Override
	public boolean onSetTag(String key, boolean value) {
		if (Tags.ERROR.getKey().equals(key)) {
			error = value;
		}
		return value;
	}

	@Override
	public void onFinish(io.opentracing.Span span, String operationName, long durationNanos) {
		if (isServer) {
			trackMetrics(operationName, durationNanos);
		}
	}

	private void trackMetrics(String operationName, long durationNanos) {
		metricRegistry.timer(getTimerMetricName(operationName)).update(durationNanos, NANOSECONDS);
		metricRegistry.timer(getTimerMetricName("All")).update(durationNanos, NANOSECONDS);

		if (requestMonitorPlugin.isCollectCpuTime()) {
			metricRegistry.timer(responseTimeCpuTemplate.build(operationName)).update(durationCpu.longValue(), MICROSECONDS);
			metricRegistry.timer(responseTimeCpuTemplate.build("All")).update(durationCpu.longValue(), MICROSECONDS);
		}

		if (error) {
			metricRegistry.meter(getErrorMetricName(operationName)).mark();
			metricRegistry.meter(getErrorMetricName("All")).mark();
		}
	}

	public static MetricName getErrorMetricName(String requestName) {
		return errorRateTemplate.build(requestName);
	}

	public static MetricName getTimerMetricName(String requestName) {
		return timerMetricNameTemplate.build(requestName);
	}
}
