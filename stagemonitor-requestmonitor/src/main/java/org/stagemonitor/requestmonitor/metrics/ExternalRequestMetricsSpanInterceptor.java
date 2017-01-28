package org.stagemonitor.requestmonitor.metrics;

import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.BasicSpanInterceptor;

import java.util.concurrent.TimeUnit;

import io.opentracing.Span;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ExternalRequestMetricsSpanInterceptor extends BasicSpanInterceptor {

	private final CorePlugin corePlugin;
	private final RequestMonitorPlugin requestMonitorPlugin;

	public static final String EXTERNAL_REQUEST_TYPE = "type";
	public static final String EXTERNAL_REQUEST_METHOD = "method";
	public static final String EXTERNAL_REQUEST_PARENT_NAME = "parent_name";

	private static final MetricName.MetricNameTemplate externalRequestRateTemplate = name("external_requests_rate")
			.templateFor("request_name", "type");
	private static final MetricName.MetricNameTemplate responseTimeExternalRequestLayerTemplate = name("response_time_server")
			.templateFor("request_name", "layer");
	private static final MetricName.MetricNameTemplate externalRequestTemplate = name("external_request_response_time")
			.templateFor("type", "signature", "method");

	private String type;
	private String method;
	private String parentName;

	public ExternalRequestMetricsSpanInterceptor(String operationName, CorePlugin corePlugin, RequestMonitorPlugin requestMonitorPlugin) {
		super(operationName);
		this.corePlugin = corePlugin;
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	@Override
	public String onSetTag(String key, String value) {
		value = super.onSetTag(key, value);
		if (EXTERNAL_REQUEST_TYPE.equals(key)) {
			type = value;
		} else if (EXTERNAL_REQUEST_METHOD.equals(key)) {
			method = value;
		} else if (EXTERNAL_REQUEST_PARENT_NAME.equals(key)) {
			parentName = value;
		}
		return value;
	}

	@Override
	public void onFinish(Span span, long endTimestampNanos) {
		super.onFinish(span, endTimestampNanos);
		if (isClient && StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(operationName) && StringUtils.isNotEmpty(method)) {
			corePlugin.getMetricRegistry()
					.timer(externalRequestTemplate.build(type, "All", method))
					.update(durationNanos, TimeUnit.NANOSECONDS);
			corePlugin.getMetricRegistry()
					.timer(externalRequestTemplate.build(type, operationName, method))
					.update(durationNanos, TimeUnit.NANOSECONDS);
			if (parentName != null) {
				trackExternalRequestMetrics(parentName, durationNanos);
			}
		}
	}

	// TODO test!
	private void trackExternalRequestMetrics(String requestName, long durationNanos) {
		final Metric2Registry metricRegistry = corePlugin.getMetricRegistry();
		if (durationNanos > 0) {
			if (requestMonitorPlugin.isCollectDbTimePerRequest()) {
				metricRegistry.timer(responseTimeExternalRequestLayerTemplate
						.build(requestName, type))
						.update(durationNanos, NANOSECONDS);
			}
			metricRegistry.timer(responseTimeExternalRequestLayerTemplate
					.build("All", type))
					.update(durationNanos, NANOSECONDS);
		}
		// the difference to ExternalRequestMetricsReporter is that the
		// external_requests_rate is grouped by the request name, not the dao method name
		metricRegistry.meter(externalRequestRateTemplate
				.build(requestName, type))
				.mark();
	}

}
