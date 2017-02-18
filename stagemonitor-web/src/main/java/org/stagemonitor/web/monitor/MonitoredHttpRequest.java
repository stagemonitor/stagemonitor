package org.stagemonitor.web.monitor;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.MonitoredRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.NoopSpan;
import org.stagemonitor.requestmonitor.utils.SpanUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;
import org.stagemonitor.web.monitor.widget.WidgetAjaxSpanReporter;
import org.stagemonitor.web.opentracing.HttpServletRequestTextMapExtractAdapter;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MonitoredHttpRequest extends MonitoredRequest {

	public static final String CONNECTION_ID_ATTRIBUTE = "connectionId";
	public static final String WIDGET_ALLOWED_ATTRIBUTE = "showWidgetAllowed";
	// has to be static so that the cache is shared between different requests
	private static UserAgentParser userAgentParser;
	protected final HttpServletRequest httpServletRequest;
	protected final FilterChain filterChain;
	protected final StatusExposingByteCountingServletResponse responseWrapper;
	protected final WebPlugin webPlugin;
	private final Metric2Registry metricRegistry;
	private final RequestMonitorPlugin requestMonitorPlugin;
	private final MetricName.MetricNameTemplate throughputMetricNameTemplate = name("request_throughput").templateFor("request_name", "http_code");
	private final String userAgenHeader;
	private final String connectionId;
	private final boolean widgetAndStagemonitorEndpointsAllowed;
	private final String clientIp;

	public MonitoredHttpRequest(HttpServletRequest httpServletRequest,
								StatusExposingByteCountingServletResponse responseWrapper,
								FilterChain filterChain, Configuration configuration) {
		this.httpServletRequest = httpServletRequest;
		this.filterChain = filterChain;
		this.responseWrapper = responseWrapper;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
		this.metricRegistry = Stagemonitor.getMetric2Registry();
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		userAgenHeader = httpServletRequest.getHeader("user-agent");
		connectionId = httpServletRequest.getHeader(WidgetAjaxSpanReporter.CONNECTION_ID);
		widgetAndStagemonitorEndpointsAllowed = webPlugin.isWidgetAndStagemonitorEndpointsAllowed(httpServletRequest, configuration);
		clientIp = getClientIp(httpServletRequest);
	}

	@Override
	public String getInstanceName() {
		return httpServletRequest.getServerName();
	}

	@Override
	public Span createSpan() {
		if (webPlugin.isHonorDoNotTrackHeader() && "1".equals(httpServletRequest.getHeader("dnt"))) {
			return NoopSpan.INSTANCE;
		}

		final Tracer tracer = requestMonitorPlugin.getTracer();
		SpanContext spanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpServletRequestTextMapExtractAdapter(httpServletRequest));
		final Span span = tracer.buildSpan(getRequestName())
				.asChildOf(spanCtx)
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
				.start();
		SpanUtils.setOperationType(span, "http");
		Tags.HTTP_URL.set(span, httpServletRequest.getRequestURI());
		span.setTag("method", httpServletRequest.getMethod());
		span.setTag("http.referring_site", getReferringSite());
		if (webPlugin.isCollectHttpHeaders()) {
			SpanUtils.setHttpHeaders(span, getHeaders(httpServletRequest));
		}

		return span;
	}

	private String getReferringSite() {
		final String refererHeader = httpServletRequest.getHeader("Referer");
		if (StringUtils.isEmpty(refererHeader)) {
			return null;
		}
		String referrerHost;
		try {
			referrerHost = new URI(refererHeader).getHost();
		} catch (URISyntaxException e) {
			referrerHost = null;
		}
		if (httpServletRequest.getServerName().equals(referrerHost)) {
			return null;
		} else {
			return referrerHost;
		}
	}

	public static String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		ip = getIpFromHeaderIfNotAlreadySet("X-Real-IP", request, ip);
		ip = getIpFromHeaderIfNotAlreadySet("Proxy-Client-IP", request, ip);
		ip = getIpFromHeaderIfNotAlreadySet("WL-Proxy-Client-IP", request, ip);
		ip = getIpFromHeaderIfNotAlreadySet("HTTP_CLIENT_IP", request, ip);
		ip = getIpFromHeaderIfNotAlreadySet("HTTP_X_FORWARDED_FOR", request, ip);
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	private static String getIpFromHeaderIfNotAlreadySet(String header, HttpServletRequest request, String ip) {
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader(header);
		}
		return ip;
	}

	public String getRequestName() {
		if (webPlugin.isMonitorOnlySpringMvcRequests() || webPlugin.isMonitorOnlyResteasyRequests()) {
			return null;
		} else {
			return getRequestNameByRequest(httpServletRequest, webPlugin);
		}
	}

	public static String getRequestNameByRequest(HttpServletRequest request, WebPlugin webPlugin) {
		String requestURI = removeSemicolonContent(request.getRequestURI().substring(request.getContextPath().length()));
		for (Map.Entry<Pattern, String> entry : webPlugin.getGroupUrls().entrySet()) {
			requestURI = entry.getKey().matcher(requestURI).replaceAll(entry.getValue());
		}
		return request.getMethod() + " " + requestURI;
	}

	private static String removeSemicolonContent(String requestUri) {
		int semicolonIndex = requestUri.indexOf(';');
		while (semicolonIndex != -1) {
			int slashIndex = requestUri.indexOf('/', semicolonIndex);
			String start = requestUri.substring(0, semicolonIndex);
			requestUri = (slashIndex != -1) ? start + requestUri.substring(slashIndex) : start;
			semicolonIndex = requestUri.indexOf(';', semicolonIndex);
		}
		return requestUri;
	}

	private Map<String, String> getHeaders(HttpServletRequest request) {
		Map<String, String> headers = new HashMap<String, String>();
		final Enumeration headerNames = request.getHeaderNames();
		final Collection<String> excludedHeaders = webPlugin.getExcludeHeaders();
		while (headerNames.hasMoreElements()) {
			final String headerName = ((String) headerNames.nextElement()).toLowerCase();
			if (!excludedHeaders.contains(headerName)) {
				headers.put(headerName, request.getHeader(headerName));
			}
		}
		return headers;
	}

	@Override
	public Object execute() throws Exception {
		filterChain.doFilter(httpServletRequest, responseWrapper);
		return null;
	}

	@Override
	public void onPostExecute(RequestMonitor.RequestInformation info) {
		final Span span = info.getSpan();

		// Search the configured exception attributes that may have been set
		// by the servlet container/framework. Use the first exception found (if any)
		for (String requestExceptionAttribute : webPlugin.getRequestExceptionAttributes()) {
			Object exception = httpServletRequest.getAttribute(requestExceptionAttribute);
			if (exception != null && exception instanceof Exception) {
				SpanUtils.setException(span, (Exception) exception, requestMonitorPlugin.getIgnoreExceptions(), requestMonitorPlugin.getUnnestExceptions());
				break;
			}
		}

		// get the parameters after the execution and not on creation, because that could lead to wrong decoded
		// parameters inside the application
		@SuppressWarnings("unchecked") // according to javadoc, its always a Map<String, String[]>
		final Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
		Map<String, String> params = new HashMap<String, String>();
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			params.put(entry.getKey(), StringUtils.toCommaSeparatedString(entry.getValue()));
		}
		Set<Pattern> confidentialParams = new HashSet<Pattern>();
		confidentialParams.addAll(webPlugin.getRequestParamsConfidential());
		confidentialParams.addAll(requestMonitorPlugin.getConfidentialParameters());
		SpanUtils.setParameters(span, RequestMonitorPlugin.getSafeParameterMap(params, confidentialParams));
	}

	@Override
	@SuppressWarnings("squid:S2696")
	public void onBeforeReport(RequestMonitor.RequestInformation requestInformation) {
		requestInformation.addRequestAttribute(CONNECTION_ID_ATTRIBUTE, connectionId);
		requestInformation.addRequestAttribute(WIDGET_ALLOWED_ATTRIBUTE, widgetAndStagemonitorEndpointsAllowed);

		final Span span = requestInformation.getSpan();

		final String userName = getUserName();
		final String sessionId = getSessionId();
		span.setTag(SpanUtils.USERNAME, userName);
		span.setTag("session_id", sessionId);
		if (userName != null) {
			span.setTag("tracking.unique_visitor_id", StringUtils.sha1Hash(userName));
		} else {
			span.setTag("tracking.unique_visitor_id", StringUtils.sha1Hash(clientIp + sessionId + userAgenHeader));
		}
		SpanUtils.setClientIp(span, clientIp);

		int status = responseWrapper.getStatus();
		Tags.HTTP_STATUS.set(span, status);

		metricRegistry.meter(throughputMetricNameTemplate.build(requestInformation.getOperationName(), Integer.toString(status))).mark();
		metricRegistry.meter(throughputMetricNameTemplate.build("All", Integer.toString(status))).mark();
		Tags.ERROR.set(span, status >= 400);
		span.setTag("bytes_written", responseWrapper.getContentLength());
		if (webPlugin.isParseUserAgent()) {
			// this is safe even though userAgentParser is static because onBeforeReport is not executed concurrently
			if (userAgentParser == null) {
				userAgentParser = new UserAgentParser();
			}
			userAgentParser.setUserAgentInformation(span, userAgenHeader);
		}
	}

	private String getSessionId() {
		final HttpSession session = httpServletRequest.getSession(false);
		return session != null ? session.getId() : null;
	}

	private String getUserName() {
		final Principal userPrincipal = httpServletRequest.getUserPrincipal();
		return userPrincipal != null ? userPrincipal.getName() : null;
	}

	/**
	 * In a web context, we only want to monitor forwarded requests.
	 * If a request to /a makes a
	 * {@link javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
	 * to /b, we only want to collect metrics for /b, because it is the request, that does the actual computation.
	 *
	 * @return true
	 */
	@Override
	public boolean isMonitorForwardedExecutions() {
		return true;
	}
}
