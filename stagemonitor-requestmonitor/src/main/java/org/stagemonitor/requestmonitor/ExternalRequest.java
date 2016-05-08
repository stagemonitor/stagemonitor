package org.stagemonitor.requestmonitor;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.stagemonitor.core.util.StringUtils;

public class ExternalRequest {

	private final double MS_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	@JsonProperty("@timestamp")
	private final String timestamp;
	@JsonIgnore
	private RequestTrace requestTrace;
	private final String requestType;
	private final String requestMethod;
	@JsonIgnore
	private final long executionTimeNanos;
	private String executedBy;
	private final String request;

	public ExternalRequest(String requestType, String requestMethod, long executionTimeNanos, String request) {
		this.requestType = requestType;
		this.requestMethod = requestMethod;
		this.executionTimeNanos = executionTimeNanos;
		this.request = request;
		this.timestamp = StringUtils.dateAsIsoString(new Date());
	}

	public void setRequestTrace(RequestTrace requestTrace) {
		this.requestTrace = requestTrace;
	}

	public String getRequestTraceId() {
		return requestTrace.getId();
	}

	public String getRequestTraceName() {
		return requestTrace.getName();
	}

	public String getTimestamp() {
		return timestamp;
	}

	public long getMeasurementStart() {
		return requestTrace.getMeasurementStart();
	}

	public String getApplication() {
		return requestTrace.getApplication();
	}

	public String getHost() {
		return requestTrace.getHost();
	}

	public String getInstance() {
		return requestTrace.getInstance();
	}

	public String getRequestType() {
		return requestType;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	@JsonIgnore
	public long getExecutionTimeNanos() {
		return executionTimeNanos;
	}

	public double getExecutionTime() {
		return executionTimeNanos / MS_IN_NANOS;
	}

	public String getExecutedBy() {
		return executedBy;
	}

	public void setExecutedBy(String executedBy) {
		this.executedBy = executedBy;
	}

	public String getRequest() {
		return request;
	}
}
