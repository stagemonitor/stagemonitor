package de.isys.jawap.model;

import java.util.ArrayList;
import java.util.List;

public class MethodCallStats {

	public final long start = System.nanoTime();
	public final MethodCallStats parent;
	private String id;
	private HttpRequestStats requestStats;
	private String className;
	private String methodName;
	private long executionTime;
	private long netExecutionTime;
	private long timestamp = System.currentTimeMillis();
	private List<MethodCallStats> children = new ArrayList<MethodCallStats>();

	public MethodCallStats(MethodCallStats parent) {
		this.parent = parent;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public HttpRequestStats getRequestStats() {
		return requestStats;
	}

	public void setRequestStats(HttpRequestStats requestStats) {
		this.requestStats = requestStats;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public long getNetExecutionTime() {
		return netExecutionTime;
	}

	public void setNetExecutionTime(long netExecutionTime) {
		this.netExecutionTime = netExecutionTime;
	}

	public List<MethodCallStats> getChildren() {
		return children;
	}

	public void setChildren(List<MethodCallStats> children) {
		this.children = children;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(className).append('.').append(methodName);
		return sb.toString();
	}

	public void addToNetExecutionTime(long executionTime) {
		netExecutionTime += executionTime;
	}

	public void subtractFromNetExecutionTime(long executionTime) {
		netExecutionTime -= executionTime;
	}
}