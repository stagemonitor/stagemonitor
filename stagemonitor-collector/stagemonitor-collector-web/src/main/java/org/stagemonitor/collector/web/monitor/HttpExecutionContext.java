package org.stagemonitor.collector.web.monitor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.stagemonitor.collector.core.monitor.ExecutionContext;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpExecutionContext extends ExecutionContext {

	private String url;
	private Integer statusCode;
	private String header;
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

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
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
		sb.append(method).append(' ').append(getUrl());
		if (getParameter() != null) {
			sb.append(getParameter());
		}
		sb.append(" (").append(statusCode).append(")\n");
		sb.append(header);

		appendCallStack(sb, asciiArt);
		return sb.toString();
	}
}
