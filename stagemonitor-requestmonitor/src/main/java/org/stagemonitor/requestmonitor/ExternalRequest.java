package org.stagemonitor.requestmonitor;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;

public class ExternalRequest {

	private final double MS_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	@JsonProperty("@timestamp")
	private final String timestamp;
	@JsonIgnore
	private RequestTrace requestTrace;
	@JsonProperty("request_type")
	private final String requestType;
	@JsonProperty("request_method")
	private final String requestMethod;
	@JsonIgnore
	private long executionTimeNanos;
	@JsonProperty("executed_by")
	private String executedBy;
	private final String request;
	@JsonIgnore
	private CallStackElement callStackElement;

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

	@JsonProperty("request_id")
	public String getRequestTraceId() {
		return requestTrace.getId();
	}

	@JsonProperty("request_name")
	public String getRequestTraceName() {
		return requestTrace.getName();
	}

	public String getTimestamp() {
		return timestamp;
	}

	@JsonProperty("measurement_start")
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

	@JsonProperty("execution_time")
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

	public void incrementExecutionTime(long additionalExecutionTime) {
		executionTimeNanos += additionalExecutionTime;
	}

	public CallStackElement getCallStackElement() {
		return callStackElement;
	}

	public void setCallStackElement(CallStackElement callStackElement) {
		this.callStackElement = callStackElement;
	}
}
