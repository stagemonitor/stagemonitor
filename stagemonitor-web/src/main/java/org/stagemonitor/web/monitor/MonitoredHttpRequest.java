package org.stagemonitor.web.monitor;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.MonitoredRequest;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.SpanContextInformation;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.tracing.wrapper.StatelessSpanEventListener;
import org.stagemonitor.requestmonitor.utils.SpanUtils;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;
import org.stagemonitor.web.monitor.widget.WidgetAjaxSpanReporter;
import org.stagemonitor.web.tracing.HttpServletRequestTextMapExtractAdapter;

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
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

public class MonitoredHttpRequest extends MonitoredRequest {

	public static final String CONNECTION_ID_ATTRIBUTE = "connectionId";
	public static final String WIDGET_ALLOWED_ATTRIBUTE = "showWidgetAllowed";
	public static final String MONITORED_HTTP_REQUEST_ATTRIBUTE = "MonitoredHttpRequest";
	// has to be static so that the cache is shared between different requests
	private static UserAgentParser userAgentParser;
	protected final HttpServletRequest httpServletRequest;
	protected final FilterChain filterChain;
	protected final StatusExposingByteCountingServletResponse responseWrapper;
	protected final WebPlugin webPlugin;
	private final RequestMonitorPlugin requestMonitorPlugin;
	private static final MetricName.MetricNameTemplate throughputMetricNameTemplate = name("request_throughput").templateFor("request_name", "http_code");
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
		requestMonitorPlugin = configuration.getConfig(RequestMonitorPlugin.class);
		userAgenHeader = httpServletRequest.getHeader("user-agent");
		connectionId = httpServletRequest.getHeader(WidgetAjaxSpanReporter.CONNECTION_ID);
		widgetAndStagemonitorEndpointsAllowed = webPlugin.isWidgetAndStagemonitorEndpointsAllowed(httpServletRequest, configuration);
		clientIp = getClientIp(httpServletRequest);
	}

	@Override
	public Span createSpan() {
		boolean sample = true;
		if (webPlugin.isHonorDoNotTrackHeader() && "1".equals(httpServletRequest.getHeader("dnt"))) {
			sample = false;
		}

		final Tracer tracer = requestMonitorPlugin.getTracer();
		io.opentracing.SpanContext spanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpServletRequestTextMapExtractAdapter(httpServletRequest));
		Tracer.SpanBuilder spanBuilder = tracer.buildSpan(getRequestName())
				.asChildOf(spanCtx)
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
		if (!sample) {
			spanBuilder = spanBuilder.withTag(Tags.SAMPLING_PRIORITY.getKey(), (short) 0);
		}
		final Span span = spanBuilder.start();
		span.setTag(SpanUtils.OPERATION_TYPE, "http");
		Tags.HTTP_URL.set(span, httpServletRequest.getRequestURI());
		Tags.PEER_PORT.set(span, httpServletRequest.getRemotePort());
		span.setTag("method", httpServletRequest.getMethod());
		span.setTag("http.referring_site", getReferringSite());
		if (webPlugin.isCollectHttpHeaders()) {
			SpanUtils.setHttpHeaders(span, getHeaders(httpServletRequest));
		}

		SpanContextInformation info = SpanContextInformation.forSpan(span);
		info.addRequestAttribute(CONNECTION_ID_ATTRIBUTE, connectionId);
		info.addRequestAttribute(WIDGET_ALLOWED_ATTRIBUTE, widgetAndStagemonitorEndpointsAllowed);
		info.addRequestAttribute(MONITORED_HTTP_REQUEST_ATTRIBUTE, this);
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
	public void execute() throws Exception {
		filterChain.doFilter(httpServletRequest, responseWrapper);
	}

	public static class HttpSpanEventListener extends StatelessSpanEventListener {

		private final WebPlugin webPlugin;
		private final RequestMonitorPlugin requestMonitorPlugin;
		private final Metric2Registry metricRegistry;

		public HttpSpanEventListener() {
			this(Stagemonitor.getPlugin(WebPlugin.class), Stagemonitor.getPlugin(RequestMonitorPlugin.class), Stagemonitor.getMetric2Registry());
		}

		public HttpSpanEventListener(WebPlugin webPlugin, RequestMonitorPlugin requestMonitorPlugin, Metric2Registry metricRegistry) {
			this.webPlugin = webPlugin;
			this.requestMonitorPlugin = requestMonitorPlugin;
			this.metricRegistry = metricRegistry;
		}

		@Override
		public void onFinish(SpanWrapper span, String operationName, long durationNanos) {
			final MonitoredHttpRequest monitoredHttpRequest = (MonitoredHttpRequest) SpanContextInformation.forSpan(span)
					.getRequestAttribute(MONITORED_HTTP_REQUEST_ATTRIBUTE);
			if (monitoredHttpRequest == null) {
				return;
			}
			trackServletExceptions(span, monitoredHttpRequest.httpServletRequest);
			setParams(span, monitoredHttpRequest.httpServletRequest);
			setTrackingInformation(span, monitoredHttpRequest.httpServletRequest, monitoredHttpRequest.clientIp, monitoredHttpRequest.userAgenHeader);
			setStatus(span, monitoredHttpRequest.responseWrapper.getStatus());
			if (operationName != null) {
				trackThroughput(operationName, monitoredHttpRequest.responseWrapper.getStatus());
			}
			span.setTag("bytes_written", monitoredHttpRequest.responseWrapper.getContentLength());
			if (webPlugin.isParseUserAgent()) {
				setUserAgentInformation(span, monitoredHttpRequest.userAgenHeader);
			}
		}

		private void setStatus(Span span, int status) {
			Tags.HTTP_STATUS.set(span, status);
			Tags.ERROR.set(span, status >= 400);
		}

		private void trackThroughput(String operationName, int status) {
			metricRegistry.meter(throughputMetricNameTemplate.build(operationName, Integer.toString(status))).mark();
			metricRegistry.meter(throughputMetricNameTemplate.build("All", Integer.toString(status))).mark();
		}

		private void setUserAgentInformation(Span span, String userAgenHeader) {
			// this is safe even though userAgentParser is static because onBeforeReport is not executed concurrently
			if (userAgentParser == null) {
				userAgentParser = new UserAgentParser();
			}
			userAgentParser.setUserAgentInformation(span, userAgenHeader);
		}

		private void setTrackingInformation(Span span, HttpServletRequest httpServletRequest, String clientIp, String userAgenHeader) {
			final String userName = getUserName(httpServletRequest);
			final String sessionId = getSessionId(httpServletRequest);
			span.setTag(SpanUtils.USERNAME, userName);
			span.setTag("session_id", sessionId);
			if (userName != null) {
				span.setTag("tracking.unique_visitor_id", StringUtils.sha1Hash(userName));
			} else {
				span.setTag("tracking.unique_visitor_id", StringUtils.sha1Hash(clientIp + sessionId + userAgenHeader));
			}
			SpanUtils.setClientIp(span, clientIp);
		}

		private void trackServletExceptions(Span span, HttpServletRequest httpServletRequest) {
			// Search the configured exception attributes that may have been set
			// by the servlet container/framework. Use the first exception found (if any)
			for (String requestExceptionAttribute : webPlugin.getRequestExceptionAttributes()) {
				Object exception = httpServletRequest.getAttribute(requestExceptionAttribute);
				if (exception != null && exception instanceof Exception) {
					SpanUtils.setException(span, (Exception) exception, requestMonitorPlugin.getIgnoreExceptions(), requestMonitorPlugin.getUnnestExceptions());
					break;
				}
			}
		}

		private void setParams(Span span, HttpServletRequest httpServletRequest) {
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

		private String getSessionId(HttpServletRequest httpServletRequest) {
			final HttpSession session = httpServletRequest.getSession(false);
			return session != null ? session.getId() : null;
		}

		private String getUserName(HttpServletRequest httpServletRequest) {
			final Principal userPrincipal = httpServletRequest.getUserPrincipal();
			return userPrincipal != null ? userPrincipal.getName() : null;
		}

	}

}
