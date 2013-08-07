package de.isys.jawap.collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledThreadPoolExecutor;

@Component
public class SpringScheduledThreadPoolMetricsCollectorImpl implements ThreadPoolMetricsCollector {

	private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

	@Autowired
	public SpringScheduledThreadPoolMetricsCollectorImpl(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
		this.scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) threadPoolTaskScheduler.getScheduledExecutor();
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
		return Integer.valueOf(scheduledThreadPoolExecutor.getQueue().size());
	}
}
