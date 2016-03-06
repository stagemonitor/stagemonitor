package org.stagemonitor.requestmonitor.reporter;

import com.codahale.metrics.Meter;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestTrace;

public class PreExecutionInterceptorContext {
	private final Configuration configuration;
	private final RequestTrace requestTrace;
	private final Meter reportingRate;
	private final Metric2Registry metricRegistry;
	private boolean mustReport = false;
	private boolean report = true;

	PreExecutionInterceptorContext(Configuration configuration, RequestTrace requestTrace, Meter reportingRate, Metric2Registry metricRegistry) {
		this.configuration = configuration;
		this.requestTrace = requestTrace;
		this.reportingRate = reportingRate;
		this.metricRegistry = metricRegistry;
	}

	public PreExecutionInterceptorContext mustReport() {
		mustReport = true;
		report = true;
		return this;
	}

	public PreExecutionInterceptorContext shouldNotReport() {
		if (!mustReport) {
			report = false;
		}
		return this;
	}

	public PreExecutionInterceptorContext addProperty(String key, Object value) {
		requestTrace.addCustomProperty(key, value);
		return this;
	}

	public RequestTrace getRequestTrace() {
		return requestTrace;
	}

	public Meter getReportingRate() {
		return reportingRate;
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
}
