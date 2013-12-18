package de.isys.jawap.entities.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.isys.jawap.entities.profiler.ExecutionContext;

import javax.persistence.Entity;
import java.util.concurrent.TimeUnit;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpRequestContext extends ExecutionContext {

	private String url;
	private String queryParams;
	private long timestamp = System.currentTimeMillis();
	private Integer statusCode;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id: ").append(getId()).append('\n');
		sb.append("Request to ").append(getUrl());
		if (getQueryParams() != null) {
			sb.append("?").append(getQueryParams());
		}
		sb.append(" took ").append(TimeUnit.NANOSECONDS.toMillis(getExecutionTime())).append(" ms\n");
		return sb.toString();
	}
}
