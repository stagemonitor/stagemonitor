package de.isys.jawap.collector.model;

public class HttpRequestStats {

	private String id;
	private PerformanceMeasurementSession performanceMeasurementSession;
	private String url;
	private String queryParams;
	private long timestamp = System.currentTimeMillis();
	private long executionTime;
	private Integer statusCode;
	private MethodCallStats methodCallStats;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public PerformanceMeasurementSession getPerformanceMeasurementSession() {
		return performanceMeasurementSession;
	}

	public void setPerformanceMeasurementSession(PerformanceMeasurementSession performanceMeasurementSession) {
		this.performanceMeasurementSession = performanceMeasurementSession;
	}

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

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public MethodCallStats getMethodCallStats() {
		return methodCallStats;
	}

	public void setMethodCallStats(MethodCallStats methodCallStats) {
		this.methodCallStats = methodCallStats;
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
}
