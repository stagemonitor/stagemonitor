package org.stagemonitor.tracing.metrics;

import com.codahale.metrics.Timer;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;
import org.stagemonitor.util.StringUtils;

import java.util.concurrent.TimeUnit;

import io.opentracing.Span;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MetricsSpanEventListener extends StatelessSpanEventListener {

	private static final double MILLISECOND_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	private static final MetricName.MetricNameTemplate responseTimeTemplate = name("response_time")
			.templateFor("operation_name", "operation_type");
	private static final MetricName.MetricNameTemplate errorRateTemplate = name("error_rate")
			.templateFor("operation_name", "operation_type");
	private static final MetricName.MetricNameTemplate externalRequestRateTemplate = name("external_requests_rate")
			.templateFor("operation_name");

	private final Metric2Registry metricRegistry;

	public MetricsSpanEventListener(Metric2Registry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final SpanContextInformation contextInformation = SpanContextInformation.forSpan(spanWrapper);
		final String operationType = contextInformation.getOperationType();
		if (StringUtils.isNotEmpty(operationName) && StringUtils.isNotEmpty(operationType)) {
			trackMetrics(contextInformation, operationName, durationNanos, operationType);
			if (contextInformation.isServerRequest()) {
				trackExternalRequestRate(spanWrapper, operationName, contextInformation);
			} else if (contextInformation.isExternalRequest()) {
				addExternalRequestToParent(durationNanos, contextInformation, operationType);
			}
		}
	}

	private void trackMetrics(SpanContextInformation contextInformation, String operationName, long durationNanos, String operationType) {
		final Timer timer = metricRegistry.timer(getResponseTimeMetricName(operationName, operationType));
		timer.update(durationNanos, NANOSECONDS);
		contextInformation.setTimerForThisRequest(timer);

		metricRegistry.timer(getResponseTimeMetricName("All", operationType)).update(durationNanos, NANOSECONDS);

		if (contextInformation.isError()) {
			metricRegistry.meter(getErrorMetricName(operationName, operationType)).mark();
			metricRegistry.meter(getErrorMetricName("All", operationType)).mark();
		} else {
			metricRegistry.meter(getErrorMetricName(operationName, operationType)).mark(0);
			metricRegistry.meter(getErrorMetricName("All", operationType)).mark(0);
		}
	}

	/*
	 * tracks the external requests grouped by the parent request name
	 * this helps to analyze which requests issue a lot of external requests like jdbc calls
	 */
	private void trackExternalRequestRate(Span span, String operationName, SpanContextInformation spanContext) {
		int totalCount = 0;
		for (SpanContextInformation.ExternalRequestStats externalRequestStats : spanContext.getExternalRequestStats()) {
			long durationNanos = externalRequestStats.getExecutionTimeNanos();
			final String requestType = externalRequestStats.getRequestType();
			span.setTag("external_requests." + requestType + ".duration_ms", durationNanos / MILLISECOND_IN_NANOS);
			span.setTag("external_requests." + requestType + ".count", externalRequestStats.getExecutionCount());
			totalCount += externalRequestStats.getExecutionCount();
		}
		metricRegistry.meter(externalRequestRateTemplate
				.build(operationName))
				.mark(totalCount);
	}

	private void addExternalRequestToParent(long durationNanos, SpanContextInformation contextInformation, String operationType) {
		final SpanContextInformation parent = contextInformation.getParent();
		if (parent != null) {
			parent.addExternalRequest(operationType, durationNanos);
		}
	}

	public static MetricName getErrorMetricName(String requestName, String operationType) {
		return errorRateTemplate.build(requestName, operationType);
	}

	public static MetricName getResponseTimeMetricName(String operationName, String operationType) {
		return responseTimeTemplate.build(operationName, operationType);
	}

}
