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
	public int getActualPoolSize() {
		return threadPool.getPoolSize();
	}

	@Override
	public int getPoolNumActive() {
		return threadPool.getActiveCount();
	}

	@Override
	public Integer getNumTasksPending() {
		return threadPool.getQueue().size();
	}
}
