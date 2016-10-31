package org.stagemonitor.web.monitor;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;

import org.stagemonitor.core.MeasurementSession;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.web.WebPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

import io.opentracing.tag.Tags;

/**
 * This class extends the generic request trace with data specific for http requests
 */
@Deprecated
public class HttpRequestTrace extends RequestTrace {

	private final UserAgentStringParser parser = UADetectorServiceFactory.getResourceModuleParser();
	private static final int MAX_ELEMENTS = 100;
	private static final Map<String, ReadableUserAgent> userAgentCache =
			new LinkedHashMap<String, ReadableUserAgent>(MAX_ELEMENTS + 1, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry eldest) {
					return size() > MAX_ELEMENTS;
				}
			};

	private final String url;
	private Integer statusCode;
	private final Map<String, String> headers;
	private final String method;
	private Integer bytesWritten;
	private UserAgentInformation userAgent;
	private String sessionId;
	@JsonIgnore
	private final String connectionId;
	@JsonIgnore
	private final boolean showWidgetAllowed;
	private String referringSite;

	public HttpRequestTrace(String requestId, String url, Map<String, String> headers, String method,
							String connectionId, boolean showWidgetAllowed) {
		this(requestId, url, headers, method, connectionId, showWidgetAllowed,
				Stagemonitor.getMeasurementSession(), Stagemonitor.getPlugin(RequestMonitorPlugin.class));
	}

	public HttpRequestTrace(String requestId, String url, Map<String, String> headers, String method,
							String connectionId, boolean showWidgetAllowed,
							MeasurementSession measurementSession, RequestMonitorPlugin requestMonitorPlugin) {
		super(requestId, measurementSession, requestMonitorPlugin);
		this.url = url;
		this.headers = headers;
		this.connectionId = connectionId;
		this.method = method;
		this.showWidgetAllowed = showWidgetAllowed;
	}

	public static class UserAgentInformation {
		private final String type;
		private final String device;
		private final String os;
		private final String osFamily;
		private final String osVersion;
		private final String browser;
		private final String browserVersion;

		public UserAgentInformation(ReadableUserAgent userAgent) {
			type = userAgent.getTypeName();
			device = userAgent.getDeviceCategory().getName();
			os = userAgent.getOperatingSystem().getName();
			osFamily = userAgent.getOperatingSystem().getFamilyName();
			osVersion = userAgent.getOperatingSystem().getVersionNumber().toVersionString();
			browser = userAgent.getName();
			browserVersion = userAgent.getVersionNumber().toVersionString();
		}

		public String getType() {
			return type;
		}

		public String getDevice() {
			return device;
		}

		public String getOs() {
			return os;
		}

		public String getOsFamily() {
			return osFamily;
		}

		public String getOsVersion() {
			return osVersion;
		}

		public String getBrowser() {
			return browser;
		}

		public String getBrowserVersion() {
			return browserVersion;
		}
	}

	public String getUrl() {
		return url;
	}

	public Integer getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(Integer statusCode) {
		Tags.HTTP_STATUS.set(span, statusCode);
		this.statusCode = statusCode;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getMethod() {
		return method;
	}

	public void setBytesWritten(Integer bytesWritten) {
		span.setTag("bytesWritten", bytesWritten);
		this.bytesWritten = bytesWritten;
	}

	public Integer getBytesWritten() {
		return bytesWritten;
	}

	public UserAgentInformation getUserAgent() {
		if (userAgent == null && headers != null && Stagemonitor.getPlugin(WebPlugin.class).isParseUserAgent()) {
			final String userAgentHeader = headers.get("user-agent");
			if (userAgentHeader != null) {
				ReadableUserAgent readableUserAgent = userAgentCache.get(userAgentHeader);
				if (readableUserAgent == null) {
					readableUserAgent = parser.parse(userAgentHeader);
					userAgentCache.put(userAgentHeader, readableUserAgent);
				}
				userAgent = new UserAgentInformation(readableUserAgent);
			}
		}
		return userAgent;
	}

	/**
	 * @return the http session id, <code>null</code> if there is no session associated with the request
	 */
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		span.setTag("sessionId", sessionId);
		this.sessionId = sessionId;
	}

	/**
	 * The connection id is used to associate ajax requests with a particular browser window in which the
	 * stagemonitor widget is running.
	 * <p/>
	 * It is used to to push request traces of ajax requests to the in browser widget.
	 *
	 * @return the connection id
	 */
	public String getConnectionId() {
		return connectionId;
	}

	public boolean isShowWidgetAllowed() {
		return showWidgetAllowed;
	}

	public void setReferringSite(String referringSite) {
		this.referringSite = referringSite;
	}

	public String getReferringSite() {
		return referringSite;
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean asciiArt, boolean callStack) {
		StringBuilder sb = new StringBuilder(3000);
		sb.append(method).append(' ').append(url);
		if (getParameters() != null) {
			sb.append(getParameters());
		}
		sb.append(" (").append(statusCode).append(")\n");
		sb.append("id:     ").append(getId()).append('\n');
		sb.append("name:   ").append(getName()).append('\n');
		if (headers != null) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				sb.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
			}
		}
		if (callStack) {
			appendCallStack(sb, asciiArt);
		}
		return sb.toString();
	}
}
