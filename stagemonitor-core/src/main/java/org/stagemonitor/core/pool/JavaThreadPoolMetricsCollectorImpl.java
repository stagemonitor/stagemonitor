package org.stagemonitor.core.pool;


import java.util.concurrent.ThreadPoolExecutor;

public class JavaThreadPoolMetricsCollectorImpl implements PooledResource {

	private final ThreadPoolExecutor threadPool;
	private final String name;

	public JavaThreadPoolMetricsCollectorImpl(ThreadPoolExecutor threadPool, String name) {
		this.threadPool = threadPool;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
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
