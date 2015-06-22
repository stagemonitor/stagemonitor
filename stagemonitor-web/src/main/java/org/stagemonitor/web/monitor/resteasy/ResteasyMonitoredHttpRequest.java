package org.stagemonitor.web.monitor.resteasy;

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.plugins.server.servlet.ServletUtil;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.BusinessTransactionNamingStrategy;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Enumeration;

/**
 * A {@link MonitoredHttpRequest} for Resteasy requests.
 *
 * <p>This class will use the Resteasy jax-rs resource class and method for naming the request.
 */
public class ResteasyMonitoredHttpRequest extends MonitoredHttpRequest {
	private static String servletMappingPrefix;

	private final RequestMonitorPlugin requestMonitorPlugin;

	public ResteasyMonitoredHttpRequest(
			HttpServletRequest httpServletRequest,
			StatusExposingByteCountingServletResponse statusExposingResponse,
			FilterChain filterChain, Configuration configuration) {
		super(httpServletRequest, statusExposingResponse, filterChain, configuration);
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
	}

	@Override
	public String getRequestName() {
		ServletContext servletContext = httpServletRequest.getServletContext();
		String servletMappingPrefix = getServletMappingPrefix(servletContext);
		Registry registry = (Registry) servletContext.getAttribute(Registry.class.getName());

		String name = "";
		if (registry == null) {
			name = super.getRequestName();
		} else {
			HttpRequest request = new RegistryLookupHttpRequest(httpServletRequest, servletMappingPrefix);
			ResourceInvoker invoker = registry.getResourceInvoker(request);
			name = getRequestNameFromInvoker(invoker, requestMonitorPlugin.getBusinessTransactionNamingStrategy());

			if (!name.isEmpty()) {
			  return name;
			}

			if (!webPlugin.isMonitorOnlyResteasyRequests()) {
				name = super.getRequestName();
			}
		}

		return name;
	}

	private String getServletMappingPrefix(ServletContext servletContext) {
		if (servletMappingPrefix != null) {
			return servletMappingPrefix;
		}

		servletMappingPrefix = servletContext
				.getInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX);
		if (servletMappingPrefix == null) {
			servletMappingPrefix = "";
		}
		servletMappingPrefix = servletMappingPrefix.trim();

		return servletMappingPrefix;
	}

	/**
	 * Gets the Resteasy request name using the given {@link ResourceInvoker} to lookup the Resteasy resource class and
	 * method.
	 *
	 * <p>The naming strategy can be specified by the {@code businessTransactionNamingStrategy} parameter. Acceptable
	 * values can be found in {@link BusinessTransactionNamingStrategy}.
	 */
	static String getRequestNameFromInvoker(ResourceInvoker invoker, BusinessTransactionNamingStrategy businessTransactionNamingStrategy) {
		if (invoker != null && invoker instanceof ResourceMethodInvoker) {
			Method resourceMethod = invoker.getMethod();
			return businessTransactionNamingStrategy.getBusinessTransationName(
					resourceMethod.getDeclaringClass().getSimpleName(), resourceMethod.getName());
		}
		return "";
	}

	/**
	 * A Resteasy {@link HttpRequest} implementation for wrapping an {@link HttpServletRequest} instance for resource
	 * class lookups using {@link Registry}.
	 */
	private static class RegistryLookupHttpRequest implements HttpRequest {
		private final HttpServletRequest httpServletRequest;
		private final ResteasyUriInfo uri;
		private final ResteasyHttpHeaders headers;

		RegistryLookupHttpRequest(HttpServletRequest httpServletRequest, String servletMappingPrefix) {
			this.httpServletRequest = httpServletRequest;
			this.uri = ServletUtil.extractUriInfo(httpServletRequest, servletMappingPrefix);
			this.headers = ServletUtil.extractHttpHeaders(httpServletRequest);
		}

		@Override
		public HttpHeaders getHttpHeaders() {
			return headers;
		}

		@Override
		public MultivaluedMap<String, String> getMutableHeaders() {
			return null;
		}

		@Override
		public InputStream getInputStream() {
			return null;
		}

		@Override
		public void setInputStream(InputStream stream) {

		}

		@Override
		public ResteasyUriInfo getUri() {
			return uri;
		}

		@Override
		public String getHttpMethod() {
			return httpServletRequest.getMethod();
		}

		@Override
		public void setHttpMethod(String method) {

		}

		@Override
		public void setRequestUri(URI requestUri) throws IllegalStateException {

		}

		@Override
		public void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException {

		}

		@Override
		public MultivaluedMap<String, String> getFormParameters() {
			return null;
		}

		@Override
		public MultivaluedMap<String, String> getDecodedFormParameters() {
			return null;
		}

		@Override
		public Object getAttribute(String attribute) {
			return httpServletRequest.getAttribute(attribute);
		}

		@Override
		public void setAttribute(String name, Object value) {
		}

		@Override
		public void removeAttribute(String name) {
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			return httpServletRequest.getAttributeNames();
		}

		@Override
		public ResteasyAsynchronousContext getAsyncContext() {
			return null;
		}

		@Override
		public boolean isInitial() {
			return true;
		}

		@Override
		public void forward(String path) {

		}

		@Override
		public boolean wasForwarded() {
			return false;
		}
	}
}
