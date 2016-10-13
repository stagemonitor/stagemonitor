package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestTrace;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import io.opentracing.Span;

import static org.stagemonitor.requestmonitor.reporter.ServerRequestMetricsReporter.getTimerMetricName;

public class PostExecutionInterceptorContext extends PreExecutionInterceptorContext {

	private final Collection<String> excludedProperties = new LinkedList<String>();

	PostExecutionInterceptorContext(Configuration configuration, RequestTrace requestTrace, Span span, Meter reportingRate, Metric2Registry metricRegistry) {
		super(configuration, requestTrace, span, reportingRate, metricRegistry);
	}

	public PostExecutionInterceptorContext addExcludedProperty(String properties) {
		excludedProperties.add(properties);
		return this;
	}

	public PostExecutionInterceptorContext addExcludedProperties(String... properties) {
		excludedProperties.addAll(Arrays.asList(properties));
		return this;
	}

	public PostExecutionInterceptorContext addProperty(String key, Object value) {
		super.addProperty(key, value);
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
		return getMetricRegistry().timer(getTimerMetricName(getRequestTrace().getName()));
	}

}
