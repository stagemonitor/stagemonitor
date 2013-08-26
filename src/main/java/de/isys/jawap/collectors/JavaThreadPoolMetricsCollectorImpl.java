package de.isys.jawap.collectors;

import java.util.concurrent.ThreadPoolExecutor;

public class JavaThreadPoolMetricsCollectorImpl implements ThreadPoolMetricsCollector {

	private ThreadPoolExecutor scheduledThreadPoolExecutor;

//	@Autowired
//	public SpringScheduledThreadPoolMetricsCollectorImpl(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
//		this.scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) threadPoolTaskScheduler.getScheduledExecutor();
//	}

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
		return Integer.valueOf(scheduledThreadPoolExecutor.getQueue().size());
	}
}
