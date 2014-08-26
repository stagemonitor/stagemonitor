package org.stagemonitor.web.monitor.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Configuration;
import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.core.util.IOUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.monitor.HttpRequestTrace;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;
import org.stagemonitor.web.monitor.QueryParameterConfigurationSource;

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
import java.io.InputStream;
import java.util.Map;

public class HttpRequestMonitorFilter extends AbstractExclusionFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequestMonitorFilter.class);
	protected final Configuration configuration;
	protected final RequestMonitor requestMonitor;
	private final QueryParameterConfigurationSource queryParameterConfigurationSource;
	private boolean servletApiForWidgetSufficient = true;

	public HttpRequestMonitorFilter() {
		this(StageMonitor.getConfiguration());
	}

	public HttpRequestMonitorFilter(Configuration configuration) {
		this.configuration = configuration;
		queryParameterConfigurationSource = new QueryParameterConfigurationSource(configuration);
		requestMonitor = new RequestMonitor(configuration);
	}

	@Override
	public void initInternal(FilterConfig filterConfig) throws ServletException {
		final MeasurementSession measurementSession = new MeasurementSession(getApplicationName(filterConfig),
				RequestMonitor.getHostName(), configuration.getInstanceName());
		requestMonitor.setMeasurementSession(measurementSession);
		configuration.addConfigurationSource(queryParameterConfigurationSource, true);

		if (configuration.isStagemonitorWidgetEnabled()) {
			final ServletContext servletContext = filterConfig.getServletContext();
			if(servletContext.getMajorVersion() >= 3) {
				servletApiForWidgetSufficient = true;
			} else {
				servletApiForWidgetSufficient = false;
				logger.error("stagemonitor.web.widget.enabled is true, but your servlet api version is not supported. " +
						"The stagemonitor widget requires servlet api 3.");
			}
		}
	}

	private String getApplicationName(FilterConfig filterConfig) {
		String name = configuration.getApplicationName();
		if (name == null || name.isEmpty()) {
			name = filterConfig.getServletContext().getServletContextName();
		}
		return name;
	}

	@Override
	public final void doFilterInternal(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
			throws IOException, ServletException {
		beforeFilter();
		if (configuration.isStagemonitorActive() && request instanceof HttpServletRequest && response instanceof HttpServletResponse && ! isInternalRequest((HttpServletRequest) request)) {
			final HttpServletRequest httpServletRequest = (HttpServletRequest) request;

			updateConfiguration(httpServletRequest);

			final StatusExposingByteCountingServletResponse responseWrapper;
			HttpServletResponseBufferWrapper httpServletResponseBufferWrapper = null;
			if (isInjectWidget(httpServletRequest)) {
				httpServletResponseBufferWrapper = new HttpServletResponseBufferWrapper((HttpServletResponse) response);
				responseWrapper = new StatusExposingByteCountingServletResponse(httpServletResponseBufferWrapper);
			} else {
				responseWrapper = new StatusExposingByteCountingServletResponse((HttpServletResponse) response);
			}

			try {
				final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation = monitorRequest(filterChain, httpServletRequest, responseWrapper);
				if (isInjectWidget(httpServletRequest)) {
					injectWidget(response, httpServletRequest, httpServletResponseBufferWrapper, requestInformation);
				}
			} catch (Exception e) {
				handleException(e);
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}

	private boolean isInjectWidget(HttpServletRequest httpServletRequest) {
		return isStagemonitorWidgetEnabled() && isHtmlRequested(httpServletRequest);
	}

	private boolean isHtmlRequested(HttpServletRequest httpServletRequest) {
		final String accept = httpServletRequest.getHeader("accept");
		return accept != null && accept.contains("text/html");
	}

	private boolean isStagemonitorWidgetEnabled() {
		return configuration.isStagemonitorWidgetEnabled() && servletApiForWidgetSufficient;
	}

	private boolean isInternalRequest(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/stagemonitor");
	}

	protected void beforeFilter() {
	}

	protected RequestMonitor.RequestInformation<HttpRequestTrace> monitorRequest(FilterChain filterChain, HttpServletRequest httpServletRequest, StatusExposingByteCountingServletResponse responseWrapper) throws Exception {
		final MonitoredHttpRequest monitoredRequest = new MonitoredHttpRequest(httpServletRequest, responseWrapper, filterChain, configuration);
		return requestMonitor.monitor(monitoredRequest);
	}

	protected void injectWidget(ServletResponse response, HttpServletRequest httpServletRequest,
	                            HttpServletResponseBufferWrapper httpServletResponseBufferWrapper,
	                            RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation) throws IOException {
		if (httpServletResponseBufferWrapper.getContentType() != null
				&& httpServletResponseBufferWrapper.getContentType().contains("text/html")
				&& httpServletRequest.getAttribute("stagemonitorWidgetInjected") == null
				&& httpServletResponseBufferWrapper.isUsingWriter()) {

			httpServletRequest.setAttribute("stagemonitorWidgetInjected", true);
			String content = httpServletResponseBufferWrapper.getWriter().getOutput().toString();
			content = injectBeforeClosingBody(content, buildWidget(requestInformation, httpServletRequest));
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

	private void updateConfiguration(HttpServletRequest httpServletRequest) {
		@SuppressWarnings("unchecked")
		final Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();

		final String configurationUpdatePassword = getFirstOrEmpty(parameterMap.get("stagemonitor.configuration.update.password"));
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			if ("stagemonitorReloadConfig".equals(entry.getKey())) {
				configuration.reload();
			} else if (entry.getKey().startsWith("stagemonitor.")) {
				queryParameterConfigurationSource.updateConfiguration(entry.getKey(), getFirstOrEmpty(entry.getValue()), configurationUpdatePassword);
			}
		}
	}

	private String getFirstOrEmpty(String[] strings) {
		if (strings != null && strings.length > 0) {
			return strings[0];
		}
		return "";
	}

	private String injectBeforeClosingBody(String unmodifiedContent, String contentToInject) {
		final int lastClosingBodyIndex = unmodifiedContent.lastIndexOf("</body>");
		final String modifiedContent;
		if(lastClosingBodyIndex > -1) {
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

	private String buildWidget(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation, HttpServletRequest httpServletRequest) throws IOException {
		final InputStream widgetStream = getClass().getClassLoader().getResourceAsStream("stagemonitorWidget.html");
		return IOUtils.toString(widgetStream)
				.replace("@@JSON_REQUEST_TACE_PLACEHOLDER@@", requestInformation.getRequestTrace().toJson())
				.replace("@@CONTEXT_PREFIX_PATH@@", httpServletRequest.getContextPath());
	}

	protected void handleException(Exception e) throws IOException, ServletException  {
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
