package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.utils.SpanTags;

import java.util.concurrent.TimeUnit;

import io.opentracing.Span;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ExternalRequestMetricsReporter extends SpanReporter {

	public static final String EXTERNAL_REQUEST_TYPE = "type";
	public static final String EXTERNAL_REQUEST_METHOD = "method";

	private static final MetricName.MetricNameTemplate externalRequestRateTemplate = name("external_requests_rate")
			.templateFor("request_name", "type");
	private static final MetricName.MetricNameTemplate responseTimeExternalRequestLayerTemplate = name("response_time_server")
			.templateFor("request_name", "layer");
	private static final MetricName.MetricNameTemplate externalRequestTemplate = name("external_request_response_time")
			.templateFor("type", "signature", "method");

	private CorePlugin corePlugin;
	private RequestMonitorPlugin requestMonitorPlugin;

	@Override
	public void init(InitArguments initArguments) {
		corePlugin = initArguments.getConfiguration().getConfig(CorePlugin.class);
		requestMonitorPlugin = initArguments.getConfiguration().getConfig(RequestMonitorPlugin.class);
	}

	@Override
	public void report(ReportArguments reportArguments) {
		final Span span = reportArguments.getSpan();
		if (SpanTags.isExternalRequest(span)) {
			trackExternalRequestMetrics((com.uber.jaeger.Span) span);
		}
	}

	private void trackExternalRequestMetrics(com.uber.jaeger.Span span) {
		// 0 means the time could not be determined
		if (span.getDuration() <= 0) {
			return;
		}
		corePlugin.getMetricRegistry()
				.timer(getExternalRequestTimerName(span, "All"))
				.update(span.getDuration(), TimeUnit.MICROSECONDS);
		corePlugin.getMetricRegistry()
				.timer(getExternalRequestTimerName(span))
				.update(span.getDuration(), TimeUnit.MICROSECONDS);
		final String parentName = (String) span.getTags().get("parent_name");
		if (parentName != null) {
			trackExternalRequestMetrics(parentName, span);
		}
	}

	// TODO test!
	private <T extends RequestTrace> void trackExternalRequestMetrics(String requestName, com.uber.jaeger.Span externalRequest) {
		final Metric2Registry metricRegistry = corePlugin.getMetricRegistry();
		final String type = externalRequest.getTags().get("type").toString();
		if (externalRequest.getDuration() > 0) {
			if (requestMonitorPlugin.isCollectDbTimePerRequest()) {
				metricRegistry.timer(responseTimeExternalRequestLayerTemplate
						.build(requestName, type))
						.update(externalRequest.getDuration(), MICROSECONDS);
			}
			metricRegistry.timer(responseTimeExternalRequestLayerTemplate
					.build("All", type))
					.update(externalRequest.getDuration(), MICROSECONDS);
		}
		// the difference to ExternalRequestMetricsReporter is that the
		// external_requests_rate is grouped by the request name, not the dao method name
		metricRegistry.meter(externalRequestRateTemplate
				.build(requestName, type))
				.mark();
	}

	public static MetricName getExternalRequestTimerName(com.uber.jaeger.Span externalRequest) {
		return getExternalRequestTimerName(externalRequest, externalRequest.getOperationName());
	}

	public static MetricName getExternalRequestTimerName(com.uber.jaeger.Span externalRequest, String signature) {
		final String type = externalRequest.getTags().get(EXTERNAL_REQUEST_TYPE).toString();
		final String method = externalRequest.getTags().get(EXTERNAL_REQUEST_METHOD).toString();
		return externalRequestTemplate.build(type, signature, method);
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
