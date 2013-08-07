package de.isys.jawap.collectors;

public interface ThreadPoolMetricsCollector {
	int getMaxPoolSize();

	int getThreadPoolSize();

	int getThreadPoolNumActiveThreads();

	Integer getThreadPoolNumTasksPending();
}
