package de.isys.jawap.collector.model;

public class ThreadPoolMetrics {

	private String id;
	private int maxPoolSize;
	private int threadPoolSize;
	private int threadPoolNumActiveThreads;
	private Integer threadPoolNumTasksPending;

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public int getThreadPoolNumActiveThreads() {
		return threadPoolNumActiveThreads;
	}

	public void setThreadPoolNumActiveThreads(int threadPoolNumActiveThreads) {
		this.threadPoolNumActiveThreads = threadPoolNumActiveThreads;
	}

	public Integer getThreadPoolNumTasksPending() {
		return threadPoolNumTasksPending;
	}

	public void setThreadPoolNumTasksPending(Integer threadPoolNumTasksPending) {
		this.threadPoolNumTasksPending = threadPoolNumTasksPending;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}
}
