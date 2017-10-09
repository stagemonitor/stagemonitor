package org.stagemonitor.tracing.metrics;

import com.codahale.metrics.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;
import org.stagemonitor.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MetricsSpanEventListener extends StatelessSpanEventListener implements Closeable {

	/**
	 * Only if a spans has this special tag, a timer for it's operation name will be created.
	 * <p>
	 * That makes sure that a timer is only created if the request name is meaningful enough. Otherwise, a timer could
	 * be created for each distinct url of the application which would result in too many timers being created.
	 */
	public static final String ENABLE_TRACKING_METRICS_TAG = SpanWrapper.INTERNAL_TAG_PREFIX + "track_metrics_per_operation_name";

	private static final Logger logger = LoggerFactory.getLogger(MetricsSpanEventListener.class);
	private static final double MILLISECOND_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);
	private static final MetricName.MetricNameTemplate responseTimeTemplate = name("response_time")
			.templateFor("operation_name", "operation_type");
	private static final MetricName.MetricNameTemplate errorRateTemplate = name("error_rate")
			.templateFor("operation_name", "operation_type");
	private static final MetricName.MetricNameTemplate externalRequestRateTemplate = name("external_requests_rate")
			.templateFor("operation_name");

	private final Metric2Registry metricRegistry;
	private final ThreadPoolExecutor executorService;
	private final TracingPlugin tracingPlugin;

	public MetricsSpanEventListener(Metric2Registry metricRegistry, ThreadPoolExecutor executorService, TracingPlugin tracingPlugin) {
		this.metricRegistry = metricRegistry;
		this.executorService = executorService;
		this.tracingPlugin = tracingPlugin;
	}

	@Override
	public void onFinish(final SpanWrapper spanWrapper, final String operationName, final long durationNanos) {
		final SpanContextInformation contextInformation = SpanContextInformation.forSpan(spanWrapper);
		final String operationType = contextInformation.getOperationType();

		final boolean trackMetricsByOperationName = spanWrapper.getBooleanTag(ENABLE_TRACKING_METRICS_TAG, false);
		if (StringUtils.isNotEmpty(operationName) && StringUtils.isNotEmpty(operationType)) {
			trackResponseTimeMetricsAsync(operationName, durationNanos, spanWrapper.getBooleanTag(Tags.ERROR.getKey(), false), operationType, trackMetricsByOperationName);
			if (SpanUtils.isServerRequest(spanWrapper)) {
				trackExternalRequestRate(spanWrapper, operationName, contextInformation, trackMetricsByOperationName);
			} else if (SpanUtils.isExternalRequest(spanWrapper)) {
				addExternalRequestToParent(durationNanos, contextInformation, operationType);
			}
		}
	}

	private void trackResponseTimeMetricsAsync(final String operationName, final long durationNanos, final boolean error, final String operationType, final boolean trackMetricsByOperationName) {
		try {
			// tracking metrics in a single thread to reduce latency and contention of the locks in ExponentiallyDecayingReservoir
			if (tracingPlugin.isTrackMetricsAsync() && executorService.getQueue().remainingCapacity() != 0) {
				executorService.submit(new Runnable() {
					@Override
					public void run() {
						trackResponseTimeMetrics(error, operationName, durationNanos, operationType, trackMetricsByOperationName);
					}
				});
			} else {
				trackResponseTimeMetrics(error, operationName, durationNanos, operationType, trackMetricsByOperationName);
			}
		} catch (RejectedExecutionException e) {
			// race condition
			logger.warn("Queue for metric tracking executor service is full, tracking metric synchronously");
			trackResponseTimeMetrics(error, operationName, durationNanos, operationType, trackMetricsByOperationName);
		}
	}

	private void trackResponseTimeMetrics(boolean error, String operationName, long durationNanos, String operationType, boolean trackMetricsByOperationName) {
		if (trackMetricsByOperationName) {
			metricRegistry.timer(getResponseTimeMetricName(operationName, operationType)).update(durationNanos, NANOSECONDS);
		}
		metricRegistry.timer(getResponseTimeMetricName("All", operationType)).update(durationNanos, NANOSECONDS);

		if (error) {
			if (trackMetricsByOperationName) {
				metricRegistry.meter(getErrorMetricName(operationName, operationType)).mark();
			}
			metricRegistry.meter(getErrorMetricName("All", operationType)).mark();
		} else {
			if (trackMetricsByOperationName) {
				metricRegistry.meter(getErrorMetricName(operationName, operationType)).mark(0);
			}
			metricRegistry.meter(getErrorMetricName("All", operationType)).mark(0);
		}
	}

	/*
	 * tracks the external requests grouped by the parent request name
	 * this helps to analyze which requests issue a lot of external requests like jdbc calls
	 */
	private void trackExternalRequestRate(Span span, String operationName, SpanContextInformation spanContext, boolean trackMetricsByOperationName) {
		int totalCount = 0;
		for (SpanContextInformation.ExternalRequestStats externalRequestStats : spanContext.getExternalRequestStats()) {
			long durationNanos = externalRequestStats.getExecutionTimeNanos();
			final String requestType = externalRequestStats.getRequestType();
			span.setTag("external_requests." + requestType + ".duration_ms", durationNanos / MILLISECOND_IN_NANOS);
			span.setTag("external_requests." + requestType + ".count", externalRequestStats.getExecutionCount());
			totalCount += externalRequestStats.getExecutionCount();
		}
		if (trackMetricsByOperationName) {
			metricRegistry.meter(externalRequestRateTemplate
					.build(operationName))
					.mark(totalCount);
		}
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

	public static Timer getTimer(Metric2Registry metricRegistry, SpanContextInformation contextInformation) {
		final String operationName = contextInformation.getOperationName();
		final String operationType = contextInformation.getOperationType();
		if (StringUtils.isNotEmpty(operationName) && StringUtils.isNotEmpty(operationType)) {
			return (Timer) metricRegistry.getMetrics().get(getResponseTimeMetricName(operationName, operationType));
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		executorService.shutdown();
	}
}
