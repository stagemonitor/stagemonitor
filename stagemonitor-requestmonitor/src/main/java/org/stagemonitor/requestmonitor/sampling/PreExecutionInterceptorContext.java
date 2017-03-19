package org.stagemonitor.requestmonitor.sampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.SpanContextInformation;

import io.opentracing.Span;

public class PreExecutionInterceptorContext {

	private static final Logger logger = LoggerFactory.getLogger(PreExecutionInterceptorContext.class);

	private final Configuration configuration;
	private final SpanContextInformation spanContext;
	private final Metric2Registry metricRegistry;
	private boolean mustReport = false;
	private boolean report = true;

	public PreExecutionInterceptorContext(Configuration configuration, SpanContextInformation spanContext, Metric2Registry metricRegistry) {
		this.configuration = configuration;
		this.spanContext = spanContext;
		this.metricRegistry = metricRegistry;
	}

	public PreExecutionInterceptorContext mustReport(Class<?> interceptorClass) {
		logger.debug("Must report current span (requested by {})", interceptorClass);
		mustReport = true;
		report = true;
		return this;
	}

	public PreExecutionInterceptorContext shouldNotReport(Class<?> interceptorClass) {
		logger.debug("Should not report current span (requested by {})", interceptorClass);
		if (!mustReport) {
			report = false;
		}
		return this;
	}

	public Span getSpan() {
		return spanContext.getSpan();
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

	public SpanContextInformation getSpanContext() {
		return spanContext;
	}
}
