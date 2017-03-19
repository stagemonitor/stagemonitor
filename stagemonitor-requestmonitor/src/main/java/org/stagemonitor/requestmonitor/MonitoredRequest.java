package org.stagemonitor.requestmonitor;

import io.opentracing.Span;

@Deprecated
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
	 * <p/>
	 * Any exception thrown by this method will be propagated (not ignored). Sometimes, methods that are required to
	 * create the execution context, like {@link javax.servlet.http.HttpServletRequest#getParameterMap()} can throw
	 * exceptions (for example, if the maximum number of parameters is exceeded).
	 * <p/>
	 * So be careful, that no exceptions are thrown in to the implementation of this method.
	 *
	 * @return the {@link Span}
	 * @param spanContext
	 */
	public abstract Span createSpan(SpanContextInformation spanContext);

	/**
	 * Executing this method triggers the execution of the execution context.
	 *
	 * @return the result of the execution
	 * @throws Exception
	 */
	public abstract void execute() throws Exception;

}
