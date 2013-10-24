package de.isys.jawap.collector.model;

import java.util.ArrayList;
import java.util.List;

public class MethodCallStats {

	public final long start = System.nanoTime();
	public final MethodCallStats parent;
	private String id;
	private String className;
	private String signature;
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

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void addToNetExecutionTime(long executionTime) {
		netExecutionTime += executionTime;
	}

	public void subtractFromNetExecutionTime(long executionTime) {
		netExecutionTime -= executionTime;
	}
}