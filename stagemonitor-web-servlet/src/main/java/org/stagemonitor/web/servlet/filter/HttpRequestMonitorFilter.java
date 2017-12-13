package org.stagemonitor.web.servlet.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.CorePlugin;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.RequestMonitor;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.web.servlet.DefaultMonitoredHttpRequestFactory;
import org.stagemonitor.web.servlet.MonitoredHttpRequest;
import org.stagemonitor.web.servlet.MonitoredHttpRequestFactory;
import org.stagemonitor.web.servlet.ServletPlugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.DispatcherType.REQUEST;

public class HttpRequestMonitorFilter extends AbstractExclusionFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequestMonitorFilter.class);
	protected final ConfigurationRegistry configuration;
	protected final CorePlugin corePlugin;
	protected final ServletPlugin servletPlugin;
	protected final RequestMonitor requestMonitor;
	private final List<HtmlInjector> htmlInjectors = new ArrayList<HtmlInjector>();
	private boolean atLeastServletApi3 = false;
	private final MonitoredHttpRequestFactory monitoredHttpRequestFactory;
	private final SpanWrapperCaptor spanWrapperCaptor;

	public HttpRequestMonitorFilter() {
		this(Stagemonitor.getConfiguration());
	}

	public HttpRequestMonitorFilter(ConfigurationRegistry configuration) {
		super(configuration.getConfig(ServletPlugin.class).getExcludedRequestPaths(), configuration.getConfig(ServletPlugin.class).getExcludedRequestPathsAntPattern());
		logger.debug("Instantiating HttpRequestMonitorFilter");
		this.configuration = configuration;
		this.servletPlugin = configuration.getConfig(ServletPlugin.class);
		this.corePlugin = configuration.getConfig(CorePlugin.class);
		this.requestMonitor = configuration.getConfig(TracingPlugin.class).getRequestMonitor();

		final Iterator<MonitoredHttpRequestFactory> requestFactoryIterator = ServiceLoader
				.load(MonitoredHttpRequestFactory.class, HttpRequestMonitorFilter.class.getClassLoader())
				.iterator();
		if (!requestFactoryIterator.hasNext()) {
			this.monitoredHttpRequestFactory = new DefaultMonitoredHttpRequestFactory(corePlugin);
		} else {
			this.monitoredHttpRequestFactory = requestFactoryIterator.next();
		}
		spanWrapperCaptor = new SpanWrapperCaptor();
		configuration.getConfig(TracingPlugin.class).addSpanEventListenerFactory(spanWrapperCaptor);
	}

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		final ServletContext servletContext = filterConfig.getServletContext();
		atLeastServletApi3 = servletContext.getMajorVersion() >= 3;

		for (HtmlInjector htmlInjector : ServiceLoader.load(HtmlInjector.class, HttpRequestMonitorFilter.class.getClassLoader())) {
			htmlInjector.init(new HtmlInjector.InitArguments(configuration, servletContext));
			htmlInjectors.add(htmlInjector);
		}
	}

	@Override
	public final void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
			throws IOException, ServletException {
		if (corePlugin.isStagemonitorActive() && !isInternalRequest(request) &&
				request.getDispatcherType() == REQUEST) {
			try {
				doMonitor(request, response, filterChain);
			} finally {
				spanWrapperCaptor.clear();
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}

	private void doMonitor(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

		final StatusExposingByteCountingServletResponse responseWrapper;
		HttpServletResponseBufferWrapper httpServletResponseBufferWrapper = null;
		if (isInjectContentToHtml(request)) {
			httpServletResponseBufferWrapper = new HttpServletResponseBufferWrapper(response);
			responseWrapper = new StatusExposingByteCountingServletResponse(httpServletResponseBufferWrapper);
		} else {
			responseWrapper = new StatusExposingByteCountingServletResponse(response);
		}

		try {
			monitorRequest(filterChain, request, responseWrapper);
		} catch (Exception e) {
			handleException(e);
		} finally {
			if (isInjectContentToHtml(request)) {
				injectHtml(response, request, httpServletResponseBufferWrapper);
			}
		}
	}

	private boolean isInjectContentToHtml(HttpServletRequest httpServletRequest) {
		if (logger.isDebugEnabled()) {
			logger.debug("atLeastServletApi3={} isHtmlRequested={} isAtLeastOneHtmlInjectorActive={}",
					atLeastServletApi3, isHtmlRequested(httpServletRequest), isAtLeastOneHtmlInjectorActive(httpServletRequest));
		}
		return atLeastServletApi3 && isHtmlRequested(httpServletRequest) && isAtLeastOneHtmlInjectorActive(httpServletRequest);
	}

	private boolean isAtLeastOneHtmlInjectorActive(HttpServletRequest httpServletRequest) {
		for (HtmlInjector htmlInjector : htmlInjectors) {
			if (htmlInjector.isActive(new HtmlInjector.IsActiveArguments(httpServletRequest))) {
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

	protected void monitorRequest(FilterChain filterChain, HttpServletRequest httpServletRequest, StatusExposingByteCountingServletResponse responseWrapper) throws Exception {
		final MonitoredHttpRequest monitoredRequest = monitoredHttpRequestFactory.createMonitoredHttpRequest(httpServletRequest, responseWrapper, filterChain, configuration);
		requestMonitor.monitor(monitoredRequest);
	}

	protected void injectHtml(HttpServletResponse response, HttpServletRequest httpServletRequest,
							  HttpServletResponseBufferWrapper httpServletResponseBufferWrapper) throws IOException {
		logger.debug("injectHtml: contentType={}", httpServletResponseBufferWrapper.getContentType());
		if (httpServletResponseBufferWrapper.getContentType() != null
				&& httpServletResponseBufferWrapper.getContentType().contains("text/html")
				&& httpServletRequest.getAttribute("stagemonitorInjected") == null) {
			if (httpServletResponseBufferWrapper.isUsingWriter()) {
				injectHtmlToWriter(response, httpServletRequest, httpServletResponseBufferWrapper);
			} else {
				injectHtmlToOutputStream(response, httpServletRequest, httpServletResponseBufferWrapper);
			}
		} else {
			passthrough(response, httpServletResponseBufferWrapper);
		}
	}

	private void injectHtmlToOutputStream(HttpServletResponse response, HttpServletRequest httpServletRequest,
										  HttpServletResponseBufferWrapper httpServletResponseBufferWrapper) throws IOException {

		logger.debug("injectHtmlToOutputStream - encoding={}", response.getCharacterEncoding());
		String content = new String(httpServletResponseBufferWrapper.getOutputStream().getOutput().toByteArray(),
				response.getCharacterEncoding());
		if (content.contains("</body>")) {
			httpServletRequest.setAttribute("stagemonitorInjected", true);
			content = getContentToInject(httpServletRequest, content);
			final byte[] bytes = content.getBytes(response.getCharacterEncoding());
			response.setContentLength(bytes.length);
			response.getOutputStream().write(bytes);
		} else {
			// this is no html
			passthrough(response, httpServletResponseBufferWrapper);
		}
	}

	private void injectHtmlToWriter(ServletResponse response, HttpServletRequest httpServletRequest, HttpServletResponseBufferWrapper httpServletResponseBufferWrapper) throws IOException {
		logger.debug("injectHtmlToWriter - encoding={}", response.getCharacterEncoding());
		httpServletRequest.setAttribute("stagemonitorInjected", true);
		String content = httpServletResponseBufferWrapper.getWriter().getOutput().toString();
		content = getContentToInject(httpServletRequest, content);
		response.getWriter().write(content);
	}

	private String getContentToInject(HttpServletRequest httpServletRequest, String content) {
		for (HtmlInjector htmlInjector : htmlInjectors) {
			if (htmlInjector.isActive(new HtmlInjector.IsActiveArguments(httpServletRequest))) {
				final HtmlInjector.InjectArguments injectArguments = new HtmlInjector.InjectArguments(spanWrapperCaptor.getSpanWrapper());
				try {
					htmlInjector.injectHtml(injectArguments);
				} catch (Exception e) {
					logger.warn(e.getMessage() + "(this exception was suppressed)", e);
				}
				content = injectBefore("</head>", content, injectArguments.getContentToInjectBeforeClosingHead(), false);
				content = injectBefore("</body>", content, injectArguments.getContentToInjectBeforeClosingBody(), true);
			}
		}
		return content;
	}

	private void passthrough(ServletResponse originalResponse, HttpServletResponseBufferWrapper responseWrapper) throws IOException {
		if (originalResponse.isCommitted()) {
			return;
		}
		if (responseWrapper.isUsingWriter()) {
			originalResponse.getWriter().write(responseWrapper.getWriter().getOutput().toString());
		} else {
			ByteArrayOutputStream output = responseWrapper.getOutputStream().getOutput();
			output.writeTo(originalResponse.getOutputStream());
		}
	}

	private String injectBefore(String tag, String unmodifiedContent, final String contentToInject, boolean lastOccurrenceOfTag) {
		final int tagIndex;
		if (lastOccurrenceOfTag) {
			tagIndex = unmodifiedContent.lastIndexOf(tag);
		} else {
			tagIndex = unmodifiedContent.indexOf(tag);
		}
		final String modifiedContent;
		if (contentToInject != null && tagIndex > -1) {
			modifiedContent = unmodifiedContent.substring(0, tagIndex) +
					contentToInject +
					unmodifiedContent.substring(tagIndex);
		} else {
			// tag not found or no content to inject - pass through without injection
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

	@Override
	public void destroy() {
		Stagemonitor.shutDown();
	}
}
