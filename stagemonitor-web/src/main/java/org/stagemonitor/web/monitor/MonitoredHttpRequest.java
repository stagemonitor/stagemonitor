package org.stagemonitor.web.monitor;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.MonitoredRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.logging.MDCListener;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;
import org.stagemonitor.web.monitor.widget.RequestTraceServlet;

public class MonitoredHttpRequest implements MonitoredRequest<HttpRequestTrace> {

	protected final HttpServletRequest httpServletRequest;
	protected final FilterChain filterChain;
	protected final StatusExposingByteCountingServletResponse responseWrapper;
	private final Configuration configuration;
	protected final WebPlugin webPlugin;
	private final Metric2Registry metricRegistry;

	public MonitoredHttpRequest(HttpServletRequest httpServletRequest,
								StatusExposingByteCountingServletResponse responseWrapper,
								FilterChain filterChain, Configuration configuration) {
		this.httpServletRequest = httpServletRequest;
		this.filterChain = filterChain;
		this.responseWrapper = responseWrapper;
		this.configuration = configuration;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
		this.metricRegistry = Stagemonitor.getMetric2Registry();
	}

	@Override
	public String getInstanceName() {
		return httpServletRequest.getServerName();
	}

	@Override
	public HttpRequestTrace createRequestTrace() {
		Map<String, String> headers = null;
		if (webPlugin.isCollectHttpHeaders()) {
			headers = getHeaders(httpServletRequest);
		}
		final String url = httpServletRequest.getRequestURI();
		final String method = httpServletRequest.getMethod();
		final String sessionId = httpServletRequest.getRequestedSessionId();
		final String connectionId = httpServletRequest.getHeader(RequestTraceServlet.CONNECTION_ID);
		final String requestId = (String) httpServletRequest.getAttribute(MDCListener.STAGEMONITOR_REQUEST_ID_ATTR);
		final boolean isShowWidgetAllowed = webPlugin.isWidgetAndStagemonitorEndpointsAllowed(httpServletRequest, configuration);
		HttpRequestTrace request = new HttpRequestTrace(requestId, url, headers, method, sessionId,
				connectionId, isShowWidgetAllowed);

		request.setReferringSite(getReferringSite());
		request.setName(getRequestName());
		String clientIp = getClientIp(httpServletRequest);
		final Principal userPrincipal = httpServletRequest.getUserPrincipal();
		request.setAndAnonymizeUserNameAndIp(userPrincipal != null ? userPrincipal.getName() : null, clientIp);

		return request;
	}

	private String getReferringSite() {
		try {
			final String host = new URI(httpServletRequest.getHeader("Referer")).getHost();
			return host.startsWith("www.") ? host.substring(4) : host;
		} catch (URISyntaxException e) {
			return null;
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

	private boolean isParamExcluded(String queryParameter) {
		final Collection<Pattern> confidentialQueryParams = webPlugin.getRequestParamsConfidential();
		for (Pattern excludedParam : confidentialQueryParams) {
			if (excludedParam.matcher(queryParameter).matches()) {
				return true;
			}
		}
		return false;
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
	public void onPostExecute(RequestMonitor.RequestInformation<HttpRequestTrace> info) {
		int status = responseWrapper.getStatus();
		HttpRequestTrace request = info.getRequestTrace();
		request.setStatusCode(status);
		metricRegistry.meter(name("request_throughput").tag("request_name", info.getRequestName()).tag("http_code", status).build()).mark();
		metricRegistry.meter(name("request_throughput").tag("request_name", "All").tag("http_code", status).build()).mark();
		if (status >= 400) {
			request.setError(true);
		}

		// Search the configured exception attributes that may have been set
		// by the servlet container/framework. Use the first exception found (if any)
		for (String requestExceptionAttribute : webPlugin.getRequestExceptionAttributes()) {
			Object exception = httpServletRequest.getAttribute(requestExceptionAttribute);
			if (exception != null && exception instanceof Exception) {
				request.setException((Exception) exception);
				break;
			}
		}
		
		request.setBytesWritten(responseWrapper.getContentLength());

		// get the parameters after the execution and not on creation, because that could lead to wrong decoded
		// parameters inside the application
		@SuppressWarnings("unchecked") // according to javadoc, its always a Map<String, String[]>
		final Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
		request.setParameters(getSafeQueryStringMap(parameterMap));
	}

	private Map<String, String> getSafeQueryStringMap(Map<String, String[]> parameterMap) {
		Map<String, String> params = new HashMap<String, String>();
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			final boolean paramExcluded = isParamExcluded(entry.getKey());
			if (paramExcluded) {
				params.put(entry.getKey(), "XXXX");
			} else {
				params.put(entry.getKey(), StringUtils.toCommaSeparatedString(entry.getValue()));
			}
		}
		return params;
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
