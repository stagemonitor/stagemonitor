package org.stagemonitor.requestmonitor;

public interface RequestTraceReporter {

	/**
	 * Callback method that is called when a {@link RequestTrace} was created and is ready to be reported
	 *
	 * @param requestTrace the {@link RequestTrace} of the current request
	 */
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) throws Exception;

	/**
	 * Whether this {@link RequestTraceReporter} is active
	 *
	 * @return <code>true</code>, if this {@link RequestTraceReporter} is active, <code>false</code> otherwise
	 */
	public <T extends RequestTrace> boolean isActive(T requestTrace);

}
