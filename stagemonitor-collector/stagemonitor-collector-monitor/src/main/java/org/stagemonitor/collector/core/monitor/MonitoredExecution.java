package org.stagemonitor.collector.core.monitor;

import org.stagemonitor.entities.profiler.ExecutionContext;

public interface MonitoredExecution<T extends ExecutionContext> {

	/**
	 * Optionally, the instance name can be derived from a {@link ExecutionContext}.
	 * For instance in a HTTP context you could return the domain name.
	 *
	 * @return the instance name
	 */
	String getInstanceName();

	/**
	 * Creates a instance of {@link ExecutionContext} that represents the current execution like a HTTP request.
	 * If <code>null</code> is returned, or the {@link ExecutionContext#name} is empty, the execution context
	 * will not be monitored.
	 *
	 * @return the {@link ExecutionContext}
	 */
	T createExecutionContext();

	/**
	 * Executing this method triggers the execution of the execution context.
	 *
	 * @return the result of the execution
	 * @throws Exception
	 */
	Object execute() throws Exception;

	/**
	 * Implement this method to do actions after the execution context
	 *
	 * @param executionContext the execution context
	 */
	void onPostExecute(T executionContext);

	/**
	 * If the current execution context triggers another one, the first one is called the forwarding execution and the
	 * second one is the forwarded execution.
	 *
	 * Implementers of this interface have to decide whether only to monitor the forwarding or forwarded execution.
	 *
	 * @return true, if only the forwarded , false, if only the forwarding execution shall be monitored.
	 */
	boolean isMonitorForwardedExecutions();
}
