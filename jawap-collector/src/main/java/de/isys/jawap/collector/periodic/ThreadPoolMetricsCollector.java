package de.isys.jawap.collector.periodic;

public interface ThreadPoolMetricsCollector {
	int getMaxPoolSize();

	int getThreadPoolSize();

	int getThreadPoolNumActiveThreads();

	Integer getThreadPoolNumTasksPending();
}
