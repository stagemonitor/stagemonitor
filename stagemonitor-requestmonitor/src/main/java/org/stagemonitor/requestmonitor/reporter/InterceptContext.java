package org.stagemonitor.requestmonitor.reporter;

import static org.stagemonitor.requestmonitor.RequestMonitor.getTimerMetricName;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestTrace;

public class InterceptContext {

	private final Configuration configuration;
	private final RequestTrace requestTrace;
	private final Meter reportingRate;
	private final Metric2Registry metricRegistry;
	private boolean mustReport = false;
	private boolean report = true;
	private final Collection<String> excludedProperties = new LinkedList<String>();

	InterceptContext(Configuration configuration, RequestTrace requestTrace, Meter reportingRate, Metric2Registry metricRegistry) {
		this.configuration = configuration;
		this.requestTrace = requestTrace;
		this.reportingRate = reportingRate;
		this.metricRegistry = metricRegistry;
	}

	public InterceptContext mustReport() {
		mustReport = true;
		report = true;
		return this;
	}

	public InterceptContext shouldNotReport() {
		if (!mustReport) {
			report = false;
		}
		return this;
	}

	public InterceptContext addExcludedProperty(String properties) {
		excludedProperties.add(properties);
		return this;
	}

	public InterceptContext addExcludedProperties(String... properties) {
		excludedProperties.addAll(Arrays.asList(properties));
		return this;
	}

	public InterceptContext addProperty(String key, Object value) {
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

	public Collection<String> getExcludedProperties() {
		return excludedProperties;
	}

	public Metric2Registry getMetricRegistry() {
		return metricRegistry;
	}

	/**
	 * Returns the timer for the current request.
	 *
	 * @return the timer for the current request (may be <code>null</code>)
	 */
	public Timer getTimerForThisRequest() {
		return metricRegistry.getTimers().get(getTimerMetricName(requestTrace.getName()));
	}

	public <T extends ConfigurationOptionProvider> T getConfig(Class<T> configClass) {
		return configuration.getConfig(configClass);
	}
}
