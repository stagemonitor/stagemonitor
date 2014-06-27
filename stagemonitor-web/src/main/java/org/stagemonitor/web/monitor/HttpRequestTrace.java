package org.stagemonitor.web.monitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;
import org.stagemonitor.core.StageMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpRequestTrace extends RequestTrace {

	private final UserAgentStringParser parser = UADetectorServiceFactory.getResourceModuleParser();
	private final static int maxElements = 100;
	private final static Map<String, ReadableUserAgent> userAgentCache =
			new LinkedHashMap<String, ReadableUserAgent>(maxElements + 1, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry eldest) {
					return size() > maxElements;
				}
			};

	private String url;
	private Integer statusCode;
	private Map<String, String> headers;
	@JsonIgnore
	private ReadableUserAgent agent;
	private String method;
	private String username;
	private String clientIp;
	private Integer bytesWritten;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
		if (headers != null && StageMonitor.getConfiguration().isParseUserAgent()) {
			final String userAgent = headers.get("user-agent");
			if (userAgent != null) {
				final ReadableUserAgent readableUserAgent = userAgentCache.get(userAgent);
				if (readableUserAgent != null) {
					agent = readableUserAgent;
				} else {
					agent = parser.parse(userAgent);
					userAgentCache.put(userAgent, agent);
				}
			}
		}
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setBytesWritten(Integer bytesWritten) {
		this.bytesWritten = bytesWritten;
	}

	public Integer getBytesWritten() {
		return bytesWritten;
	}

	public String getBrowser() {
		if (agent == null) {
			return null;
		}
		return agent.getName();
	}

	public String getBrowserVersion() {
		if (agent == null) {
			return null;
		}
		return agent.getVersionNumber().toVersionString();
	}

	public String getDevice() {
		if (agent == null) {
			return null;
		}
		return agent.getDeviceCategory().getName();
	}

	public String getUserAgentType() {
		if (agent == null) {
			return null;
		}
		return agent.getTypeName();
	}

	public String getOs() {
		if (agent == null) {
			return null;
		}
		return agent.getOperatingSystem().getName();
	}

	public String getOsFamily() {
		if (agent == null) {
			return null;
		}
		return agent.getOperatingSystem().getFamilyName();
	}

	public String getOsVersion() {
		if (agent == null) {
			return null;
		}
		return agent.getOperatingSystem().getVersionNumber().toVersionString();
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean asciiArt) {
		StringBuilder sb = new StringBuilder(3000);
		sb.append(method).append(' ').append(url);
		if (getParameter() != null) {
			sb.append(getParameter());
		}
		sb.append(" (").append(statusCode).append(")\n");
		sb.append("id:     ").append(getId()).append('\n');
		sb.append("name:   ").append(getName()).append('\n');
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			sb.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
		}
		appendCallStack(sb, asciiArt);
		return sb.toString();
	}
}
