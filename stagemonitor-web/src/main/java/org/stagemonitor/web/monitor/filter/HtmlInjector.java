package org.stagemonitor.web.monitor.filter;

import javax.servlet.ServletContext;

import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.monitor.HttpRequestTrace;

/**
 * An absraction to inject content into all text/html documents
 * <p/>
 * To register a implementation, create the file
 * src/main/resources/META-INF/services/org.stagemonitor.web.monitor.filter.HtmlInjector
 * and insert the canonical class name of the implementation.
 */
public interface HtmlInjector {

	/**
	 * Initialisation method that is called just after the Implementation was initialized
	 *
	 * @param configuration the configuration
	 * @param servletContext the current servlet context
	 */
	void init(Configuration configuration, ServletContext servletContext);

	/**
	 * Implementations can return html snippets that are injected just before the closing body tag.
	 * <p/>
	 * <b>Note:</b> {@link org.stagemonitor.requestmonitor.RequestMonitor.RequestInformation#getRequestTrace()} may be null
	 *
	 * @param requestInformation information about the current request
	 * @return the code to inject into html documents just before the closing body tag
	 */
	String getContentToInjectBeforeClosingBody(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation);

	/**
	 * Returns <code>true</code>, if this {@link HtmlInjector} should be applied, <code>false</code> otherwise
	 *
	 * @return <code>true</code>, if this {@link HtmlInjector} should be applied, <code>false</code> otherwise
	 */
	boolean isActive();
}
