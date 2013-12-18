package de.isys.jawap.collector.jvm;


import java.util.concurrent.ThreadPoolExecutor;

public class JavaThreadPoolMetricsCollectorImpl implements ThreadPoolMetricsCollector {

	private ThreadPoolExecutor scheduledThreadPoolExecutor;

	public void setScheduledThreadPoolExecutor(ThreadPoolExecutor scheduledThreadPoolExecutor) {
		this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
	}

	@Override
	public int getMaxPoolSize() {
		return scheduledThreadPoolExecutor.getCorePoolSize();
	}

	@Override
	public int getThreadPoolSize() {
		return scheduledThreadPoolExecutor.getPoolSize();
	}

	@Override
	public int getThreadPoolNumActiveThreads() {
		return scheduledThreadPoolExecutor.getActiveCount();
	}

	@Override
	public Integer getThreadPoolNumTasksPending() {
		return scheduledThreadPoolExecutor.getQueue().size();
	}
}
