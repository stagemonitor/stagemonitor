package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import io.opentracing.Span;

import static org.stagemonitor.requestmonitor.reporter.ServerRequestMetricsReporter.getTimerMetricName;

public class PostExecutionInterceptorContext extends PreExecutionInterceptorContext {

	private final Collection<String> excludedProperties = new LinkedList<String>();

	PostExecutionInterceptorContext(Configuration configuration, Span span, Meter internalRequestReportingRate, Meter externalRequestReportingRate, Metric2Registry metricRegistry) {
		super(configuration, span, internalRequestReportingRate, externalRequestReportingRate, metricRegistry);
	}

	public PostExecutionInterceptorContext addExcludedProperty(String properties) {
		excludedProperties.add(properties);
		return this;
	}

	public PostExecutionInterceptorContext addExcludedProperties(String... properties) {
		excludedProperties.addAll(Arrays.asList(properties));
		return this;
	}

	public PreExecutionInterceptorContext mustReport(Class<?> interceptorClass) {
		super.mustReport(interceptorClass);
		return this;
	}

	public PreExecutionInterceptorContext shouldNotReport(Class<?> interceptorClass) {
		super.shouldNotReport(interceptorClass);
		return this;
	}

	public Collection<String> getExcludedProperties() {
		return excludedProperties;
	}

	/**
	 * Returns the timer for the current request.
	 *
	 * @return the timer for the current request
	 */
	public Timer getTimerForThisRequest() {
		return getMetricRegistry().timer(getTimerMetricName(getInternalSpan().getOperationName()));
	}

}
