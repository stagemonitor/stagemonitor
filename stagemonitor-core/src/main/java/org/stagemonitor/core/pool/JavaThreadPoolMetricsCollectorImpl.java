package org.stagemonitor.core.pool;


import java.util.concurrent.ThreadPoolExecutor;

import org.stagemonitor.core.metrics.metrics2.MetricName;

public class JavaThreadPoolMetricsCollectorImpl implements PooledResource {

	private final ThreadPoolExecutor threadPool;
	private final String name;

	public JavaThreadPoolMetricsCollectorImpl(ThreadPoolExecutor threadPool, String name) {
		this.threadPool = threadPool;
		this.name = name;
	}

	@Override
	public MetricName getName() {
		return MetricName.name("thread-pool").tag("name", name).build();
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
