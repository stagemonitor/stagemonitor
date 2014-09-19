package org.stagemonitor.web.monitor.filter;

import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.monitor.HttpRequestTrace;

/**
 * An absraction to inject content into all text/html documents
 */
public interface HtmlInjector {

	/**
	 * Returns <code>true</code>, if this {@link HtmlInjector} should be applied, <code>false</code> otherwise
	 *
	 * @return <code>true</code>, if this {@link HtmlInjector} should be applied, <code>false</code> otherwise
	 */
	boolean isActive();

	/**
	 * Each implementation has different parameters that must be set on every request.
	 *
	 * @return the code to inject into html documents
	 */
	String build(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation);
}
