package org.stagemonitor.web.servlet.filter;

import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.tracing.SpanContextInformation;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * An SPI to inject content into all text/html documents
 * <p/>
 * To register a implementation, create the file
 * src/main/resources/META-INF/services/org.stagemonitor.web.servlet.filter.HtmlInjector
 * and insert the canonical class name of the implementation.
 */
public abstract class HtmlInjector implements StagemonitorSPI {

	/**
	 * Initialisation method that is called just after the implementation is instantiated
	 *
	 * @param initArguments
	 */
	public void init(InitArguments initArguments) {
	}

	/**
	 * Implementations can return html snippets that are injected just before the closing body tag.
	 *
	 * @param injectArguments
	 */
	public abstract void injectHtml(InjectArguments injectArguments);

	/**
	 * Returns <code>true</code>, if this {@link HtmlInjector} should be applied, <code>false</code> otherwise
	 *
	 * @return <code>true</code>, if this {@link HtmlInjector} should be applied, <code>false</code> otherwise
	 * @param isActiveArguments
	 */
	public abstract boolean isActive(IsActiveArguments isActiveArguments);

	public static class InitArguments {
		private final ConfigurationRegistry configuration;
		private final ServletContext servletContext;

		/**
		 * @param configuration the configuration
		 * @param servletContext the current servlet context
		 */
		public InitArguments(ConfigurationRegistry configuration, ServletContext servletContext) {
			this.configuration = configuration;
			this.servletContext = servletContext;
		}

		public ConfigurationRegistry getConfiguration() {
			return configuration;
		}

		public ServletContext getServletContext() {
			return servletContext;
		}
	}

	public static class InjectArguments {
		private final SpanContextInformation spanContext;
		private String contentToInjectBeforeClosingBody;

		/**
		 * @param spanContext information about the current request
		 */
		public InjectArguments(SpanContextInformation spanContext) {
			this.spanContext = spanContext;
		}

		public SpanContextInformation getSpanContext() {
			return spanContext;
		}

		public void setContentToInjectBeforeClosingBody(String contentToInject) {
			this.contentToInjectBeforeClosingBody = contentToInject;
		}

		public String getContentToInjectBeforeClosingBody() {
			return contentToInjectBeforeClosingBody;
		}
	}

	public static class IsActiveArguments {
		private final HttpServletRequest httpServletRequest;

		/**
		 */
		IsActiveArguments(HttpServletRequest httpServletRequest) {
			this.httpServletRequest = httpServletRequest;
		}

		public HttpServletRequest getHttpServletRequest() {
			return httpServletRequest;
		}
	}
}
