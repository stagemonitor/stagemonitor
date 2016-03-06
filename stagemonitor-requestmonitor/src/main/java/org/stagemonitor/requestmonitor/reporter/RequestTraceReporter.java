package org.stagemonitor.requestmonitor.reporter;

import org.stagemonitor.requestmonitor.RequestTrace;

public interface RequestTraceReporter {

	/**
	 * Callback method that is called when a {@link RequestTrace} was created and is ready to be reported
	 *
	 * @param requestTrace the {@link RequestTrace} of the current request
	 */
	<T extends RequestTrace> void reportRequestTrace(T requestTrace) throws Exception;

	/**
	 * Whether this {@link RequestTraceReporter} is active
	 * <p/>
	 * This method is called at most once from {@link org.stagemonitor.requestmonitor.RequestMonitor} for one request.
	 * That means that the result from the first evaluation is final.
	 *
	 * @return <code>true</code>, if this {@link RequestTraceReporter} is active, <code>false</code> otherwise
	 */
	<T extends RequestTrace> boolean isActive(T requestTrace);

}
