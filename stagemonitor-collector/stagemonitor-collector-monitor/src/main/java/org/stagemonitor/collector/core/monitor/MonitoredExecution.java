package org.stagemonitor.collector.core.monitor;

import org.stagemonitor.entities.profiler.ExecutionContext;

public interface MonitoredExecution<T extends ExecutionContext> {

	String getInstanceName();

	T createExecutionContext();

	Object execute() throws Exception;

	void onPostExecute(T executionContext);

	boolean isMonitorForwardedExecutions();
}
