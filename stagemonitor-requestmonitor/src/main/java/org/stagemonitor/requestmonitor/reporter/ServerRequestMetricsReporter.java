package org.stagemonitor.requestmonitor.reporter;

import com.uber.jaeger.Span;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import io.opentracing.tag.Tags;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ServerRequestMetricsReporter extends SpanReporter {

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

	private Metric2Registry metricRegistry;
	private RequestMonitorPlugin requestMonitorPlugin;

	@Override
	public void init(InitArguments initArguments) {
		metricRegistry = initArguments.getMetricRegistry();
		requestMonitorPlugin = initArguments.getConfiguration().getConfig(RequestMonitorPlugin.class);
	}

	@Override
	public void report(RequestMonitor.RequestInformation requestInformation) throws Exception {
		if (!requestInformation.isExternalRequest()) {
			trackMetrics(SpanUtils.getInternalSpan(requestInformation.getSpan()));
		}
	}

	private void trackMetrics(Span span) {
		String requestName = span.getOperationName();
		metricRegistry.timer(getTimerMetricName(requestName)).update(span.getDuration(), MICROSECONDS);
		metricRegistry.timer(getTimerMetricName("All")).update(span.getDuration(), MICROSECONDS);

		if (requestMonitorPlugin.isCollectCpuTime()) {
			metricRegistry.timer(responseTimeCpuTemplate.build(requestName)).update((Long) span.getTags().get("duration_cpu"), MICROSECONDS);
			metricRegistry.timer(responseTimeCpuTemplate.build("All")).update((Long) span.getTags().get("duration_cpu"), MICROSECONDS);
		}

		if (Boolean.TRUE.equals(span.getTags().get(Tags.ERROR.getKey()))) {
			metricRegistry.meter(getErrorMetricName(requestName)).mark();
			metricRegistry.meter(getErrorMetricName("All")).mark();
		}
	}

	public static MetricName getErrorMetricName(String requestName) {
		return errorRateTemplate.build(requestName);
	}

	public static MetricName getTimerMetricName(String requestName) {
		return timerMetricNameTemplate.build(requestName);
	}

	@Override
	public boolean isActive(RequestMonitor.RequestInformation requestInformation) {
		return true;
	}

	@Override
	public boolean requiresCallTree() {
		return false;
	}
}
