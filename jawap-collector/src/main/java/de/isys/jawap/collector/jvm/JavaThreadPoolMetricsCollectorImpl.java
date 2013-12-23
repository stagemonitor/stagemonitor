package de.isys.jawap.collector.jvm;


import de.isys.jawap.collector.core.ApplicationContext;

import java.util.concurrent.ThreadPoolExecutor;

public class JavaThreadPoolMetricsCollectorImpl implements ThreadPoolMetricsCollector {

	private ThreadPoolExecutor threadPool;

	public void setScheduledThreadPoolExecutor(ThreadPoolExecutor threadPool) {
		this.threadPool = threadPool;
	}

	@Override
	public int getMaxPoolSize() {
		return threadPool.getCorePoolSize();
	}

	@Override
	public int getThreadPoolSize() {
		return threadPool.getPoolSize();
	}

	@Override
	public int getThreadPoolNumActiveThreads() {
		return threadPool.getActiveCount();
	}

	@Override
	public Integer getThreadPoolNumTasksPending() {
		return threadPool.getQueue().size();
	}
}
