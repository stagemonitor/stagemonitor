package org.stagemonitor.requestmonitor;

import com.google.common.util.concurrent.ListenableFuture;

public interface MonitoredRequest<T extends RequestTrace> {

	/**
	 * Optionally, the instance name can be derived from a {@link RequestTrace}.
	 * For instance in a HTTP context you could return the domain name.
	 *
	 * @return the instance name
	 */
	String getInstanceName();

	/**
	 * Creates a instance of {@link RequestTrace} that represents the current request, e.g. a HTTP request.
	 * If <code>null</code> is returned, or the {@link RequestTrace#name} is empty, the execution context
	 * will not be monitored.
	 * <p/>
	 * Any exception thrown by this method will be propagated (not ignored). Sometimes, methods that are required to
	 * create the execution context, like {@link javax.servlet.http.HttpServletRequest#getParameterMap()} can throw
	 * exceptions (for example, if the maximum number of parameters is exceeded). These exceptions have to be
	 * propagated and not swallowed.
	 * <p/>
	 * So be careful, that no exceptions are thrown due to the implementation of this method.
	 *
	 * @return the {@link RequestTrace}
	 */
	T createRequestTrace();

	/**
	 * Executing this method triggers the execution of the execution context.
	 *
	 * @return the result of the execution
	 * @throws Exception
	 */
	Object execute() throws Exception;

	ListenableFuture<Object> executeAsync();

	/**
	 * Implement this method to do actions after the execution context
	 *
	 * @param requestInformation the execution context
	 */
	void onPostExecute(RequestMonitor.RequestInformation<T> requestInformation);

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
