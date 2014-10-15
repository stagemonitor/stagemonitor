package org.stagemonitor.web.monitor.filter;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.rum.BommerangJsHtmlInjector;
import org.stagemonitor.web.monitor.widget.StagemonitorWidgetHtmlInjector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpRequestMonitorFilter extends AbstractExclusionFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequestMonitorFilter.class);
	protected final Configuration configuration;
	protected final CorePlugin corePlugin;
	protected final WebPlugin webPlugin;
	protected final RequestMonitor requestMonitor;
	protected final MetricRegistry metricRegistry;
	private final List<HtmlInjector> htmlInjectors = new ArrayList<HtmlInjector>();
	private boolean atLeastServletApi3 = false;

	public HttpRequestMonitorFilter() {
		this(StageMonitor.getConfiguration(), new RequestMonitor(StageMonitor.getConfiguration()), StageMonitor.getMetricRegistry());
	}

	public HttpRequestMonitorFilter(Configuration configuration, RequestMonitor requestMonitor, MetricRegistry metricRegistry) {
		this.configuration = configuration;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.requestMonitor = requestMonitor;
		this.metricRegistry = metricRegistry;
	}

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		final MeasurementSession measurementSession = new MeasurementSession(getApplicationName(filterConfig),
				RequestMonitor.getHostName(), corePlugin.getInstanceName());
		requestMonitor.setMeasurementSession(measurementSession);
		final ServletContext servletContext = filterConfig.getServletContext();
		atLeastServletApi3 = servletContext.getMajorVersion() >= 3;

		htmlInjectors.add(new BommerangJsHtmlInjector(webPlugin, servletContext.getContextPath()));
		htmlInjectors.add(new StagemonitorWidgetHtmlInjector(configuration, webPlugin, servletContext.getContextPath()));
	}

	private String getApplicationName(FilterConfig filterConfig) {
		String name = corePlugin.getApplicationName();
		if (name == null || name.isEmpty()) {
			name = filterConfig.getServletContext().getServletContextName();
		}
		return name;
	}

	@Override
	public final void doFilterInternal(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
			throws IOException, ServletException {
		setCachingHeadersForBommerangJs(request, response);
		beforeFilter();
		if (corePlugin.isStagemonitorActive() && request instanceof HttpServletRequest &&
				response instanceof HttpServletResponse && !isInternalRequest((HttpServletRequest) request)) {
			doMonitor((HttpServletRequest) request, response, filterChain);
		} else {
			filterChain.doFilter(request, response);
		}
	}

	private void doMonitor(HttpServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

		final StatusExposingByteCountingServletResponse responseWrapper;
		HttpServletResponseBufferWrapper httpServletResponseBufferWrapper = null;
		if (isInjectContentToHtml(request)) {
			httpServletResponseBufferWrapper = new HttpServletResponseBufferWrapper((HttpServletResponse) response);
			responseWrapper = new StatusExposingByteCountingServletResponse(httpServletResponseBufferWrapper);
		} else {
			responseWrapper = new StatusExposingByteCountingServletResponse((HttpServletResponse) response);
		}

		try {
			final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = monitorRequest(filterChain, request, responseWrapper);
			if (isInjectContentToHtml(request)) {
				injectHtml(response, request, httpServletResponseBufferWrapper, requestInformation);
			}
		} catch (Exception e) {
			handleException(e);
		}
	}

	private void setCachingHeadersForBommerangJs(ServletRequest request, ServletResponse response) {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			if (httpServletRequest.getRequestURI().endsWith(BommerangJsHtmlInjector.BOOMERANG_FILENAME)) {
				final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
				httpServletResponse.setHeader("cache-control", "public, max-age=315360000");
			}
		}
	}

	private boolean isInjectContentToHtml(HttpServletRequest httpServletRequest) {
		return atLeastServletApi3 && isHtmlRequested(httpServletRequest) && isAtLeastOneHtmlInjectorActive() ;
	}

	private boolean isAtLeastOneHtmlInjectorActive() {
		for (HtmlInjector htmlInjector : htmlInjectors) {
			if (htmlInjector.isActive()) {
				return true;
			}
		}
		return false;
	}

	private boolean isHtmlRequested(HttpServletRequest httpServletRequest) {
		final String accept = httpServletRequest.getHeader("accept");
		return accept != null && accept.contains("text/html");
	}

	private boolean isInternalRequest(HttpServletRequest request) {
		return request.getRequestURI().startsWith(request.getContextPath() + "/stagemonitor");
	}

	protected void beforeFilter() {
	}

	protected RequestMonitor.RequestInformation<HttpRequestTrace> monitorRequest(FilterChain filterChain, HttpServletRequest httpServletRequest, StatusExposingByteCountingServletResponse responseWrapper) throws Exception {
		final MonitoredHttpRequest monitoredRequest = new MonitoredHttpRequest(httpServletRequest, responseWrapper, filterChain, configuration);
		return requestMonitor.monitor(monitoredRequest);
	}

	protected void injectHtml(ServletResponse response, HttpServletRequest httpServletRequest,
							  HttpServletResponseBufferWrapper httpServletResponseBufferWrapper,
							  RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation) throws IOException {
		if (httpServletResponseBufferWrapper.getContentType() != null
				&& httpServletResponseBufferWrapper.getContentType().contains("text/html")
				&& httpServletRequest.getAttribute("stagemonitorInjected") == null
				&& httpServletResponseBufferWrapper.isUsingWriter()) {
			httpServletRequest.setAttribute("stagemonitorInjected", true);
			String content = httpServletResponseBufferWrapper.getWriter().getOutput().toString();
			for (HtmlInjector htmlInjector : htmlInjectors) {
				if (htmlInjector.isActive()) {
					content = injectBeforeClosingBody(content, htmlInjector.build(requestInformation));
				}
			}
			response.getWriter().write(content);
		} else {
			passthrough(response, httpServletResponseBufferWrapper);
		}
	}

	private void passthrough(ServletResponse response, HttpServletResponseBufferWrapper httpServletResponseBufferWrapper) throws IOException {
		if (httpServletResponseBufferWrapper.isUsingWriter()) {
			response.getWriter().write(httpServletResponseBufferWrapper.getWriter().getOutput().toString());
		} else {
			ByteArrayOutputStream output = httpServletResponseBufferWrapper.getOutputStream().getOutput();
			output.writeTo(response.getOutputStream());
		}
	}

	private String injectBeforeClosingBody(String unmodifiedContent, String contentToInject) {
		final int lastClosingBodyIndex = unmodifiedContent.lastIndexOf("</body>");
		final String modifiedContent;
		if (lastClosingBodyIndex > -1) {
			final StringBuilder modifiedContentStringBuilder = new StringBuilder(unmodifiedContent.length() + contentToInject.length());
			modifiedContentStringBuilder.append(unmodifiedContent.substring(0, lastClosingBodyIndex));
			modifiedContentStringBuilder.append(contentToInject);
			modifiedContentStringBuilder.append(unmodifiedContent.substring(lastClosingBodyIndex));
			modifiedContent = modifiedContentStringBuilder.toString();
		} else {
			// no body close tag found - pass through without injection
			modifiedContent = unmodifiedContent;
		}
		return modifiedContent;
	}

	protected void handleException(Exception e) throws IOException, ServletException {
		if (e instanceof IOException) {
			throw (IOException) e;
		}
		if (e instanceof ServletException) {
			throw (ServletException) e;
		}
		if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else {
			throw new RuntimeException(e);
		}
	}
}
