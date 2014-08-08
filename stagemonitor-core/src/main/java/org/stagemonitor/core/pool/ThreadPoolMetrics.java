package org.stagemonitor.core.pool;

public class ThreadPoolMetrics {

	private String name;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}
}
