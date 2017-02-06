package org.stagemonitor.requestmonitor.sampling;

import com.codahale.metrics.Meter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;

import io.opentracing.Span;

public class PreExecutionInterceptorContext {

	private static final Logger logger = LoggerFactory.getLogger(PreExecutionInterceptorContext.class);

	private final Configuration configuration;
	private final RequestMonitor.RequestInformation requestInformation;
	private final Meter internalRequestReportingRate;
	private final Meter externalRequestReportingRate;
	private final Metric2Registry metricRegistry;
	private boolean mustReport = false;
	private boolean report = true;

	PreExecutionInterceptorContext(Configuration configuration, RequestMonitor.RequestInformation requestInformation, Meter internalRequestReportingRate, Meter externalRequestReportingRate, Metric2Registry metricRegistry) {
		this.configuration = configuration;
		this.requestInformation = requestInformation;
		this.externalRequestReportingRate = externalRequestReportingRate;
		this.internalRequestReportingRate = internalRequestReportingRate;
		this.metricRegistry = metricRegistry;
	}

	public PreExecutionInterceptorContext mustReport(Class<?> interceptorClass) {
		logger.debug("Must report current request trace (requested by {})", interceptorClass);
		mustReport = true;
		report = true;
		return this;
	}

	public PreExecutionInterceptorContext shouldNotReport(Class<?> interceptorClass) {
		logger.debug("Should not report current request trace (requested by {})", interceptorClass);
		if (!mustReport) {
			report = false;
		}
		return this;
	}

	public Span getSpan() {
		return requestInformation.getSpan();
	}

	public Meter getExternalRequestReportingRate() {
		return externalRequestReportingRate;
	}

	public Meter getInternalRequestReportingRate() {
		return internalRequestReportingRate;
	}

	public boolean isReport() {
		return report;
	}

	public Metric2Registry getMetricRegistry() {
		return metricRegistry;
	}

	public <T extends ConfigurationOptionProvider> T getConfig(Class<T> configClass) {
		return configuration.getConfig(configClass);
	}

	public RequestMonitor.RequestInformation getRequestInformation() {
		return requestInformation;
	}
}
