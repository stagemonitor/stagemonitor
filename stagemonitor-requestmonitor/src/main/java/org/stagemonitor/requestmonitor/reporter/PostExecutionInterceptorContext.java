package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Meter;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class PostExecutionInterceptorContext extends PreExecutionInterceptorContext {

	private final Collection<String> excludedProperties = new LinkedList<String>();

	PostExecutionInterceptorContext(Configuration configuration, RequestMonitor.RequestInformation requestInformation, Meter internalRequestReportingRate, Meter externalRequestReportingRate, Metric2Registry metricRegistry) {
		super(configuration, requestInformation, internalRequestReportingRate, externalRequestReportingRate, metricRegistry);
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

}
