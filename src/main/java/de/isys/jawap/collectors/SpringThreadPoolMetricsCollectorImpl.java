package de.isys.jawap.collectors;

import java.util.concurrent.ThreadPoolExecutor;

public class SpringThreadPoolMetricsCollectorImpl implements ThreadPoolMetricsCollector {

	private final ThreadPoolExecutor threadPoolExecutor;

	public SpringThreadPoolMetricsCollectorImpl(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolExecutor = threadPoolTaskExecutor.getThreadPoolExecutor();
	}

	@Override
	public int getMaxPoolSize() {
		return threadPoolExecutor.getMaximumPoolSize();
	}

	@Override
	public int getThreadPoolSize() {
		return threadPoolExecutor.getPoolSize();
	}

	@Override
	public int getThreadPoolNumActiveThreads() {
		return threadPoolExecutor.getActiveCount();
	}

	@Override
	public Integer getThreadPoolNumTasksPending() {
		return Integer.valueOf(threadPoolExecutor.getQueue().size());
	}
}
