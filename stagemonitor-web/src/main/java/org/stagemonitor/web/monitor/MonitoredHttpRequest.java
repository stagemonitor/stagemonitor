package org.stagemonitor.web.monitor;

import static com.codahale.metrics.MetricRegistry.name;

import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;

import com.codahale.metrics.MetricRegistry;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.MonitoredRequest;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.logging.MDCListener;
import org.stagemonitor.web.monitor.filter.StatusExposingByteCountingServletResponse;
import org.stagemonitor.web.monitor.widget.RequestTraceServlet;

public class MonitoredHttpRequest implements MonitoredRequest<HttpRequestTrace> {

	private static boolean determineRequestNameImmediately = false;
	protected final HttpServletRequest httpServletRequest;
	protected final FilterChain filterChain;
	protected final StatusExposingByteCountingServletResponse responseWrapper;
	protected final WebPlugin webPlugin;
	private final MetricRegistry metricRegistry;

	public MonitoredHttpRequest(HttpServletRequest httpServletRequest,
								StatusExposingByteCountingServletResponse responseWrapper,
								FilterChain filterChain, Configuration configuration) {
		this.httpServletRequest = httpServletRequest;
		this.filterChain = filterChain;
		this.responseWrapper = responseWrapper;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
		metricRegistry = Stagemonitor.getMetricRegistry();
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
		final RequestTrace.GetNameCallback nameCallback;
		if (determineRequestNameImmediately()) {
			final String requestName = getRequestName();
			nameCallback = new RequestTrace.GetNameCallback() {
				@Override
				public String getName() {
					return requestName;
				}
			};
		} else {
			nameCallback = new RequestTrace.GetNameCallback() {
				@Override
				public String getName() {
					return getRequestName();
				}
			};
		}
		final String requestId = (String) httpServletRequest.getAttribute(MDCListener.STAGEMONITOR_REQUEST_ID_ATTR);
		HttpRequestTrace request = new HttpRequestTrace(requestId, nameCallback, url, headers, method, sessionId, connectionId);

		request.setClientIp(getClientIp(httpServletRequest));
		final Principal userPrincipal = httpServletRequest.getUserPrincipal();
		request.setUsername(userPrincipal != null ? userPrincipal.getName() : null);

		return request;
	}

	/**
	 * In some servers, like WildFly, it is not possible to determine the request name later, because the
	 * {@link HttpServletRequest} is in a different state.
	 * <p/>
	 * For example: {@link javax.servlet.http.HttpServletRequest#getRequestURI()} initially returns '/owners/find.html',
	 * but after the execution, the request has a {@link javax.servlet.http.HttpServletRequest#getDispatcherType()} of
	 * {@link javax.servlet.DispatcherType#FORWARD} and {@link javax.servlet.http.HttpServletRequest#getRequestURI()}
	 * returns '/WEB-INF/jsp/owners/findOwners.jsp'
	 *
	 * @return <code>true</code>, if the request name has to be determined immediately, <code>false</code> otherwise
	 */
	private boolean determineRequestNameImmediately() {
		return determineRequestNameImmediately;
	}

	private String getClientIp(HttpServletRequest request) {
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

	private String getIpFromHeaderIfNotAlreadySet(String header, HttpServletRequest request, String ip) {
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader(header);
		}
		return ip;
	}

	public String getRequestName() {
		return getRequestNameByRequest(httpServletRequest, webPlugin);
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

	private String getSafeQueryString(Map<String, String[]> parameterMap) {
		StringBuilder queryStringBuilder = new StringBuilder();
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			final boolean paramExcluded = isParamExcluded(entry.getKey());
			for (String value : entry.getValue()) {
				if (queryStringBuilder.length() == 0) {
					queryStringBuilder.append('?');
				} else {
					queryStringBuilder.append('&');
				}

				queryStringBuilder.append(entry.getKey());
				if (paramExcluded) {
					queryStringBuilder.append('=').append("XXXX");
				} else {
					queryStringBuilder.append('=').append(value);
				}
			}
		}
		return queryStringBuilder.toString();
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
		metricRegistry.meter(name("request", info.getTimerName(), "server.meter.statuscode", Integer.toString(status))).mark();
		metricRegistry.meter(name("request.All.server.meter.statuscode", Integer.toString(status))).mark();
		if (status >= 400) {
			request.setError(true);
		}

		Object exception = httpServletRequest.getAttribute("exception");
		if (exception != null && exception instanceof Exception) {
			request.setException((Exception) exception);
		}
		request.setBytesWritten(responseWrapper.getContentLength());

		// get the parameters after the execution and not on creation, because that could lead to wrong decoded
		// parameters inside the application
		@SuppressWarnings("unchecked") // according to javadoc, its always a Map<String, String[]>
		final Map<String, String[]> parameterMap = (Map<String, String[]>) httpServletRequest.getParameterMap();
		request.setParameter(getSafeQueryString(parameterMap));
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

	public static class StagemonitorServletContextListener implements ServletContextListener {

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			determineRequestNameImmediately = sce.getServletContext().getServerInfo().contains("WildFly");
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
		}
	}
}
