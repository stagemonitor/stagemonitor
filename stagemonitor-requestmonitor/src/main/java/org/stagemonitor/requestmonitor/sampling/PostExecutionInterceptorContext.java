package org.stagemonitor.requestmonitor.sampling;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.requestmonitor.RequestMonitor;

public class PostExecutionInterceptorContext extends PreExecutionInterceptorContext {

	private boolean excludeCallTree = false;

	PostExecutionInterceptorContext(Configuration configuration, RequestMonitor.RequestInformation requestInformation, Metric2Registry metricRegistry) {
		super(configuration, requestInformation, metricRegistry);
	}

	public PreExecutionInterceptorContext mustReport(Class<?> interceptorClass) {
		super.mustReport(interceptorClass);
		return this;
	}

	public PreExecutionInterceptorContext shouldNotReport(Class<?> interceptorClass) {
		super.shouldNotReport(interceptorClass);
		return this;
	}

	public PostExecutionInterceptorContext excludeCallTree() {
		excludeCallTree = true;
		return this;
	}

	public boolean isExcludeCallTree() {
		return excludeCallTree;
	}
}
