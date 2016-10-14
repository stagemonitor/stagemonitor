package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.ExternalRequestStats;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ServerRequestMetricsReporter extends SpanReporter {

	private static final MetricName.MetricNameTemplate externalRequestRateTemplate = name("external_requests_rate")
			.templateFor("request_name", "type");
	private static final MetricName.MetricNameTemplate responseTimeExternalRequestLayerTemplate = name("response_time_server")
			.templateFor("request_name", "layer");
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
		metricRegistry = initArguments.getConfiguration().getConfig(CorePlugin.class).getMetricRegistry();
		requestMonitorPlugin = initArguments.getConfiguration().getConfig(RequestMonitorPlugin.class);
	}

	@Override
	public void report(ReportArguments reportArguments) throws Exception {
		if (reportArguments.getRequestTrace() != null) {
			trackMetrics(reportArguments.getRequestTrace());
		}
	}

	private void trackMetrics(RequestTrace requestTrace) {
		String requestName = requestTrace.getName();
		metricRegistry.timer(getTimerMetricName(requestName)).update(requestTrace.getExecutionTimeNanos(), NANOSECONDS);
		metricRegistry.timer(getTimerMetricName("All")).update(requestTrace.getExecutionTimeNanos(), NANOSECONDS);

		if (requestMonitorPlugin.isCollectCpuTime()) {
			metricRegistry.timer(responseTimeCpuTemplate.build(requestName)).update(requestTrace.getExecutionTimeCpuNanos(), NANOSECONDS);
			metricRegistry.timer(responseTimeCpuTemplate.build("All")).update(requestTrace.getExecutionTimeCpuNanos(), NANOSECONDS);
		}

		if (requestTrace.isError()) {
			metricRegistry.meter(getErrorMetricName(requestName)).mark();
			metricRegistry.meter(getErrorMetricName("All")).mark();
		}
		trackExternalRequestMetrics(requestName, requestTrace);
	}

	public static MetricName getErrorMetricName(String requestName) {
		return errorRateTemplate.build(requestName);
	}

	private <T extends RequestTrace> void trackExternalRequestMetrics(String requestName, T requestTrace) {
		for (ExternalRequestStats externalRequestStats : requestTrace.getExternalRequestStats()) {
			if (externalRequestStats.getExecutionTimeNanos() > 0) {
				if (requestMonitorPlugin.isCollectDbTimePerRequest()) {
					metricRegistry.timer(responseTimeExternalRequestLayerTemplate
							.build(requestName, externalRequestStats.getRequestType()))
							.update(externalRequestStats.getExecutionTimeNanos(), NANOSECONDS);
				}
				metricRegistry.timer(responseTimeExternalRequestLayerTemplate
						.build("All", externalRequestStats.getRequestType()))
						.update(externalRequestStats.getExecutionTimeNanos(), NANOSECONDS);
			}
			// the difference to ElasticsearchExternalRequestReporter is that the
			// external_requests_rate is grouped by the request name, not the dao method name
			metricRegistry.meter(externalRequestRateTemplate
					.build(requestName, externalRequestStats.getRequestType()))
					.mark(externalRequestStats.getExecutionCount());
		}
	}

	public static MetricName getTimerMetricName(String requestName) {
		return timerMetricNameTemplate.build(requestName);
	}

	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		return true;
	}

	@Override
	public boolean requiresCallTree() {
		return false;
	}
}
