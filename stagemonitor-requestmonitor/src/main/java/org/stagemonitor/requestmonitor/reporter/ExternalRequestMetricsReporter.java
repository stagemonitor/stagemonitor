package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.requestmonitor.ExternalRequest;

import java.util.concurrent.TimeUnit;

import io.opentracing.Span;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ExternalRequestMetricsReporter extends SpanReporter {

	public static final String EXTERNAL_REQUEST_TYPE = "type";
	public static final String EXTERNAL_REQUEST_METHOD = "method";

	private static final MetricName.MetricNameTemplate externalRequestTemplate = name("external_request_response_time")
			.templateFor("type", "signature", "method");

	private CorePlugin corePlugin;

	@Override
	public void init(InitArguments initArguments) {
		corePlugin = initArguments.getConfiguration().getConfig(CorePlugin.class);
	}

	@Override
	public void report(ReportArguments reportArguments) throws Exception {
		final Span span = reportArguments.getSpan();
		if (isExternalRequest(span)) {
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
	}

	public static MetricName getExternalRequestTimerName(com.uber.jaeger.Span externalRequest) {
		return getExternalRequestTimerName(externalRequest, externalRequest.getOperationName());
	}

	public static MetricName getExternalRequestTimerName(com.uber.jaeger.Span externalRequest, String signature) {
		final String type = externalRequest.getTags().get(EXTERNAL_REQUEST_TYPE).toString();
		final String method = externalRequest.getTags().get(EXTERNAL_REQUEST_METHOD).toString();
		return externalRequestTemplate.build(type, signature, method);
	}

	public static MetricName getExternalRequestTimerName(ExternalRequest externalRequest) {
		return getExternalRequestTimerName(externalRequest, externalRequest.getExecutedBy());
	}

	public static MetricName getExternalRequestTimerName(ExternalRequest externalRequest, String signature) {
		return externalRequestTemplate.build(externalRequest.getRequestType(), signature, externalRequest.getRequestMethod());
	}


	public boolean isExternalRequest(Span span) {
		if (span instanceof com.uber.jaeger.Span) {
			com.uber.jaeger.Span jaegerSpan = (com.uber.jaeger.Span) span;
			return jaegerSpan.isRPCClient() &&
					jaegerSpan.getTags().containsKey(EXTERNAL_REQUEST_TYPE) &&
					jaegerSpan.getTags().containsKey(EXTERNAL_REQUEST_METHOD);
		}
		return false;
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
