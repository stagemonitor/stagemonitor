package org.stagemonitor.collector.web.monitor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.stagemonitor.collector.core.monitor.ExecutionContext;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpExecutionContext extends ExecutionContext {

	private String url;
	private Integer statusCode;
	private Map<String, String> headers;
	private String method;

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
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
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
