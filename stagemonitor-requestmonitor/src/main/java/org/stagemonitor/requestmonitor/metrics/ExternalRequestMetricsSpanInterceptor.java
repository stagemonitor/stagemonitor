package org.stagemonitor.requestmonitor.metrics;

import com.codahale.metrics.Timer;

import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.ClientServerAwareSpanInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.opentracing.Span;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ExternalRequestMetricsSpanInterceptor extends ClientServerAwareSpanInterceptor {

	private final Metric2Registry metricRegistry;
	private final RequestMonitorPlugin requestMonitorPlugin;

	public static final String EXTERNAL_REQUEST_TYPE = "type";
	public static final String EXTERNAL_REQUEST_METHOD = "method";

	private static final MetricName.MetricNameTemplate externalRequestRateTemplate = name("external_requests_rate")
			.templateFor("request_name", "type");
	private static final MetricName.MetricNameTemplate responseTimeExternalRequestLayerTemplate = name("response_time_server")
			.templateFor("request_name", "layer");
	private static final MetricName.MetricNameTemplate externalRequestTemplate = name("external_request_response_time")
			.templateFor("type", "signature", "method");

	private String type;
	private String method;

	public ExternalRequestMetricsSpanInterceptor(Metric2Registry metricRegistry, RequestMonitorPlugin requestMonitorPlugin) {
		this.metricRegistry = metricRegistry;
		this.requestMonitorPlugin = requestMonitorPlugin;
	}

	public static Callable<SpanInterceptor> asCallable(final Metric2Registry metricRegistry, final RequestMonitorPlugin requestMonitorPlugin) {
		return new Callable<SpanInterceptor>() {
			@Override
			public SpanInterceptor call() throws Exception {
				return new ExternalRequestMetricsSpanInterceptor(metricRegistry, requestMonitorPlugin);
			}
		};
	}

	@Override
	public String onSetTag(String key, String value) {
		value = super.onSetTag(key, value);
		if (EXTERNAL_REQUEST_TYPE.equals(key)) {
			type = value;
		} else if (EXTERNAL_REQUEST_METHOD.equals(key)) {
			method = value;
		}
		return value;
	}

	@Override
	public void onFinish(Span span, String operationName, long durationNanos) {
		super.onFinish(span, operationName, durationNanos);
		if (isClient && StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(operationName) && StringUtils.isNotEmpty(method)) {
			metricRegistry.timer(externalRequestTemplate.build(type, "All", method))
					.update(durationNanos, TimeUnit.NANOSECONDS);
			final Timer timer = metricRegistry.timer(externalRequestTemplate.build(type, operationName, method));
			requestMonitorPlugin.getRequestMonitor().getRequestInformation().setTimerForThisRequest(timer);
			timer.update(durationNanos, TimeUnit.NANOSECONDS);
			final RequestMonitor.RequestInformation parent = requestMonitorPlugin.getRequestMonitor().getRequestInformation().getParent();
			if (parent != null) {
				trackExternalRequestMetricsOfParent(parent.getOperationName(), durationNanos);
			}
		}
	}

	// TODO test!
	/*
	 * tracks the external requests grouped by the parent request name
	 */
	private void trackExternalRequestMetricsOfParent(String requestName, long durationNanos) {
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

		metricRegistry.meter(externalRequestRateTemplate
				.build(requestName, type))
				.mark();
	}

}
