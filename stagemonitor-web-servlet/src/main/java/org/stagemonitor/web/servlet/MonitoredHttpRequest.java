package org.stagemonitor.web.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.configuration.ConfigurationRegistry;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.MonitoredRequest;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;
import org.stagemonitor.util.StringUtils;
import org.stagemonitor.web.servlet.filter.StatusExposingByteCountingServletResponse;
import org.stagemonitor.web.servlet.tracing.HttpServletRequestTextMapExtractAdapter;
import org.stagemonitor.web.servlet.useragent.UserAgentParser;
import org.stagemonitor.web.servlet.widget.WidgetAjaxSpanReporter;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

public class MonitoredHttpRequest extends MonitoredRequest {

	private static final Logger logger = LoggerFactory.getLogger(MonitoredHttpRequest.class);
	public static final String CONNECTION_ID_ATTRIBUTE = "connectionId";
	public static final String WIDGET_ALLOWED_ATTRIBUTE = "showWidgetAllowed";
	public static final String MONITORED_HTTP_REQUEST_ATTRIBUTE = "MonitoredHttpRequest";
	public static final String USER_AGENT_PARSED_FUTURE_ATTRIBUTE = MonitoredHttpRequest.class.getName() + ".userAgentParsedFuture";
	// has to be static so that the cache is shared between different requests
	private static UserAgentParser userAgentParser;
	protected final HttpServletRequest httpServletRequest;
	protected final FilterChain filterChain;
	protected final StatusExposingByteCountingServletResponse responseWrapper;
	protected final ServletPlugin servletPlugin;
	private final TracingPlugin tracingPlugin;
	private final String userAgentHeader;
	private final String connectionId;
	private final boolean widgetAndStagemonitorEndpointsAllowed;
	private final String clientIp;
	private final ExecutorService userAgentParsingExecutor;

	public MonitoredHttpRequest(HttpServletRequest httpServletRequest,
								StatusExposingByteCountingServletResponse responseWrapper,
								FilterChain filterChain, ConfigurationRegistry configuration, ExecutorService userAgentParsingExecutor) {
		this.httpServletRequest = httpServletRequest;
		this.filterChain = filterChain;
		this.responseWrapper = responseWrapper;
		this.servletPlugin = configuration.getConfig(ServletPlugin.class);
		tracingPlugin = configuration.getConfig(TracingPlugin.class);
		userAgentHeader = httpServletRequest.getHeader("user-agent");
		connectionId = httpServletRequest.getHeader(WidgetAjaxSpanReporter.CONNECTION_ID);
		this.userAgentParsingExecutor = userAgentParsingExecutor;
		widgetAndStagemonitorEndpointsAllowed = servletPlugin.isWidgetAndStagemonitorEndpointsAllowed(httpServletRequest, configuration);
		clientIp = getClientIp(httpServletRequest);
	}

	@Override
	public Scope createScope() {
		boolean sample = true;
		if (servletPlugin.isHonorDoNotTrackHeader() && "1".equals(httpServletRequest.getHeader("dnt"))) {
			sample = false;
		}

		final Tracer tracer = tracingPlugin.getTracer();
		io.opentracing.SpanContext spanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpServletRequestTextMapExtractAdapter(httpServletRequest));
		Tracer.SpanBuilder spanBuilder = tracer.buildSpan(getRequestName())
				.asChildOf(spanCtx)
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
		if (widgetAndStagemonitorEndpointsAllowed) {
			// use null as value so that the tag is not really set
			spanBuilder = spanBuilder.withTag(WIDGET_ALLOWED_ATTRIBUTE, (String) null);
		}
		if (!sample) {
			spanBuilder = spanBuilder.withTag(Tags.SAMPLING_PRIORITY.getKey(), 0);
		}
		spanBuilder.withTag(SpanUtils.OPERATION_TYPE, "http");
		final Scope scope = spanBuilder.startActive();
		final Span span = scope.span();
		Tags.HTTP_URL.set(span, httpServletRequest.getRequestURI());
		Tags.PEER_PORT.set(span, httpServletRequest.getRemotePort());
		span.setTag("method", httpServletRequest.getMethod());
		span.setTag("http.referring_site", getReferringSite());
		if (servletPlugin.isCollectHttpHeaders()) {
			SpanUtils.setHttpHeaders(span, getHeaders(httpServletRequest));
		}

		SpanContextInformation info = SpanContextInformation.forSpan(span);
		info.addRequestAttribute(CONNECTION_ID_ATTRIBUTE, connectionId);
		info.addRequestAttribute(MONITORED_HTTP_REQUEST_ATTRIBUTE, this);
		if (tracingPlugin.isSampled(span) && servletPlugin.isParseUserAgent() && StringUtils.isNotEmpty(userAgentHeader)) {
			parseUserAgentAsync(span, info);
		}
		return scope;
	}

	private void parseUserAgentAsync(final Span span, SpanContextInformation info) {
		try {
			final Future<Void> future = userAgentParsingExecutor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					setUserAgentInformation(span, userAgentHeader);
					return null;
				}
			});
			info.addRequestAttribute(USER_AGENT_PARSED_FUTURE_ATTRIBUTE, future);
		} catch (RejectedExecutionException e) {
			// pool is exhausted
			logger.warn("Failed to parse the User-Agent header as the thread pool is exhausted");
		}
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
		return getFirstIp(ip);
	}

	/*
	 * Can be a comma separated list if there are multiple devices in the forwarding chain
	 */
	private static String getFirstIp(String ip) {
		if (ip != null) {
			final int indexOfFirstComma = ip.indexOf(',');
			if (indexOfFirstComma != -1) {
				ip = ip.substring(0, indexOfFirstComma);
			}
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
		if (servletPlugin.isMonitorOnlySpringMvcRequests() || servletPlugin.isMonitorOnlyResteasyRequests()) {
			return null;
		} else {
			return getRequestNameByRequest(httpServletRequest, servletPlugin);
		}
	}

	public static String getRequestNameByRequest(HttpServletRequest request, ServletPlugin servletPlugin) {
		String requestURI = removeSemicolonContent(request.getRequestURI().substring(request.getContextPath().length()));
		for (Map.Entry<Pattern, String> entry : servletPlugin.getGroupUrls().entrySet()) {
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
		final Collection<String> excludedHeaders = servletPlugin.getExcludeHeaders();
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

	private void setUserAgentInformation(Span span, String userAgenHeader) {
		// this is safe even though userAgentParser is static because onBeforeReport is not executed concurrently
		if (userAgentParser == null) {
			userAgentParser = new UserAgentParser();
		}
		userAgentParser.setUserAgentInformation(span, userAgenHeader);
	}

	public static class HttpSpanEventListener extends StatelessSpanEventListener {

		private final ServletPlugin servletPlugin;
		private final TracingPlugin tracingPlugin;

		public HttpSpanEventListener() {
			this(Stagemonitor.getPlugin(ServletPlugin.class), Stagemonitor.getPlugin(TracingPlugin.class));
		}

		public HttpSpanEventListener(ServletPlugin servletPlugin, TracingPlugin tracingPlugin) {
			this.servletPlugin = servletPlugin;
			this.tracingPlugin = tracingPlugin;
		}

		@Override
		public void onFinish(SpanWrapper span, String operationName, long durationNanos) {
			final SpanContextInformation contextInfo = SpanContextInformation.forSpan(span);
			final MonitoredHttpRequest monitoredHttpRequest = contextInfo
					.getRequestAttribute(MONITORED_HTTP_REQUEST_ATTRIBUTE);
			if (monitoredHttpRequest == null) {
				return;
			}
			trackServletExceptions(span, monitoredHttpRequest.httpServletRequest);
			setParams(span, monitoredHttpRequest.httpServletRequest);
			setTrackingInformation(span, monitoredHttpRequest.httpServletRequest, monitoredHttpRequest.clientIp, monitoredHttpRequest.userAgentHeader);
			setStatus(span, monitoredHttpRequest.responseWrapper.getStatus());
			span.setTag("bytes_written", monitoredHttpRequest.responseWrapper.getContentLength());
			final Future<Void> userAgentParsedFuture = contextInfo.getRequestAttribute(USER_AGENT_PARSED_FUTURE_ATTRIBUTE);
			if (userAgentParsedFuture != null) {
				try {
					userAgentParsedFuture.get();
				} catch (Exception e) {
					logger.warn("Suppressed exception", e);
				}
			}
		}

		private void setStatus(Span span, int status) {
			Tags.HTTP_STATUS.set(span, status);
			if (status >= 400) {
				Tags.ERROR.set(span, true);
			}
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
			for (String requestExceptionAttribute : servletPlugin.getRequestExceptionAttributes()) {
				Object exception = httpServletRequest.getAttribute(requestExceptionAttribute);
				if (exception != null && exception instanceof Exception) {
					SpanUtils.setException(span, (Exception) exception, tracingPlugin.getIgnoreExceptions(), tracingPlugin.getUnnestExceptions());
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
			confidentialParams.addAll(servletPlugin.getRequestParamsConfidential());
			confidentialParams.addAll(tracingPlugin.getConfidentialParameters());
			SpanUtils.setParameters(span, TracingPlugin.getSafeParameterMap(params, confidentialParams));
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
