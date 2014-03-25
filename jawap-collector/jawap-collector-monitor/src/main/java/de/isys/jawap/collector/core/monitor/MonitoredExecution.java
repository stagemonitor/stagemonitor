package de.isys.jawap.collector.core.monitor;

import de.isys.jawap.entities.profiler.ExecutionContext;

public interface MonitoredExecution<T extends ExecutionContext> {

	String getInstanceName();

	T createExecutionContext();

	Object execute() throws Exception;

	void onPostExecute(T executionContext);

	boolean isMonitorForwardedExecutions();
}
