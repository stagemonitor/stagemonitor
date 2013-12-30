package de.isys.jawap.entities.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.isys.jawap.entities.profiler.ExecutionContext;

import javax.persistence.Entity;
import javax.persistence.Lob;
import java.util.concurrent.TimeUnit;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpRequestContext extends ExecutionContext {

	private String url;
	private String queryParams;
	private long timestamp = System.currentTimeMillis();
	private Integer statusCode;
	@Lob
	private String header;
	private String method;
	// TODO header, cpu time

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getQueryParams() {
		return queryParams;
	}

	public void setQueryParams(String queryParams) {
		this.queryParams = queryParams;
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
		StringBuilder sb = new StringBuilder();
		//sb.append("id: ").append(getId()).append('\n');
		sb.append(method).append(' ').append(getUrl());
		if (getQueryParams() != null) {
			sb.append(getQueryParams());
		}
		sb.append(" (").append(statusCode).append(")\n");
		sb.append(header);
		return sb.toString();
	}
}
