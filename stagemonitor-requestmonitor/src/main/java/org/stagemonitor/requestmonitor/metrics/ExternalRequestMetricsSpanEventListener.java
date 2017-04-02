package org.stagemonitor.requestmonitor.metrics;

import com.codahale.metrics.Timer;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.tracing.wrapper.ClientServerAwareSpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListener;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.concurrent.TimeUnit;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class ExternalRequestMetricsSpanEventListener extends ClientServerAwareSpanEventListener {

	private final Metric2Registry metricRegistry;

	public static final String EXTERNAL_REQUEST_METHOD = "method";

	private static final MetricName.MetricNameTemplate externalRequestTemplate = name("external_request_response_time")
			.templateFor("type", "signature", "method");

	private String type;
	private String method;

	public ExternalRequestMetricsSpanEventListener(Metric2Registry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	public static class Factory implements SpanEventListenerFactory {

		private final Metric2Registry metricRegistry;

		public Factory() {
			this(Stagemonitor.getMetric2Registry());
		}

		public Factory(Metric2Registry metricRegistry) {
			this.metricRegistry = metricRegistry;
		}

		@Override
		public SpanEventListener create() {
			return new ExternalRequestMetricsSpanEventListener(metricRegistry);
		}
	}

	@Override
	public String onSetTag(String key, String value) {
		value = super.onSetTag(key, value);
		if (SpanUtils.OPERATION_TYPE.equals(key)) {
			type = value;
		} else if (EXTERNAL_REQUEST_METHOD.equals(key)) {
			method = value;
		}
		return value;
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		if (isClient && StringUtils.isNotEmpty(type) && StringUtils.isNotEmpty(operationName) && StringUtils.isNotEmpty(method)) {
			metricRegistry.timer(externalRequestTemplate.build(type, "All", method))
					.update(durationNanos, TimeUnit.NANOSECONDS);
			final Timer timer = metricRegistry.timer(externalRequestTemplate.build(type, operationName, method));
			SpanContextInformation.forSpan(spanWrapper).setTimerForThisRequest(timer);
			timer.update(durationNanos, TimeUnit.NANOSECONDS);

			final SpanContextInformation parent = SpanContextInformation.forSpan(spanWrapper).getParent();
			if (parent != null) {
				parent.addExternalRequest(type, durationNanos);
			}
		}
	}

}
