package org.stagemonitor.requestmonitor;

import io.opentracing.Span;

public abstract class MonitoredRequest {

	/**
	 * Optionally, the instance name can be derived from a request.
	 * For instance in a HTTP context you could return the domain name.
	 *
	 * @return the instance name
	 */
	public String getInstanceName() {
		return null;
	}

	/**
	 * Creates a instance of {@link Span} that represents the current request, e.g. a HTTP request.
	 * If <code>null</code> is returned, or the {@link Span#setOperationName(String)} is empty, the execution context
	 * will not be monitored.
	 * <p/>
	 * Any exception thrown by this method will be propagated (not ignored). Sometimes, methods that are required to
	 * create the execution context, like {@link javax.servlet.http.HttpServletRequest#getParameterMap()} can throw
	 * exceptions (for example, if the maximum number of parameters is exceeded). These exceptions have to be
	 * propagated and not swallowed.
	 * <p/>
	 * So be careful, that no exceptions are thrown in to the implementation of this method.
	 *
	 * @return the {@link Span}
	 */
	public abstract Span createSpan();

	/**
	 * Executing this method triggers the execution of the execution context.
	 *
	 * @return the result of the execution
	 * @throws Exception
	 */
	public abstract Object execute() throws Exception;

	/**
	 * Implement this method to do actions after the execution context.
	 *
	 * This method is executed synchronously and will block the request
	 *
	 * @param requestInformation the execution context
	 */
	public void onPostExecute(RequestMonitor.RequestInformation requestInformation) {
	}

	/**
	 * Implement this method to do actions before {@link Span} is reported.
	 *
	 * This method is executed asynchronously and will not the request.
	 *
	 * @param requestInformation the execution context
	 */
	public void onBeforeReport(RequestMonitor.RequestInformation requestInformation) {
	}

	/**
	 * If the current execution context triggers another one, the first one is called the forwarding execution and the
	 * second one is the forwarded execution.
	 *
	 * Implementers of this interface have to decide whether only to monitor the forwarding or forwarded execution.
	 *
	 * @return true, if only the forwarded , false, if only the forwarding execution shall be monitored.
	 */
	public abstract boolean isMonitorForwardedExecutions();
}
