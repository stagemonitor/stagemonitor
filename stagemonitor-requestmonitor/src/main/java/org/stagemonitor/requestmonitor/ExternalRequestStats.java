package org.stagemonitor.requestmonitor;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ExternalRequestStats {

	private final double MS_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

	private final String requestType;
	private int executionCount;
	@JsonIgnore
	private long executionTimeNanos;

	public ExternalRequestStats(ExternalRequest externalRequest) {
		requestType = externalRequest.getRequestType();
		executionCount = 1;
		executionTimeNanos = externalRequest.getExecutionTimeNanos();
	}

	public double getExecutionTime() {
		return executionTimeNanos / MS_IN_NANOS;
	}

	public long getExecutionTimeNanos() {
		return executionTimeNanos;
	}

	public int getExecutionCount() {
		return executionCount;
	}

	public String getRequestType() {
		return requestType;
	}

	public void add(ExternalRequest externalRequest) {
		executionCount++;
		executionTimeNanos += externalRequest.getExecutionTimeNanos();
	}
}
