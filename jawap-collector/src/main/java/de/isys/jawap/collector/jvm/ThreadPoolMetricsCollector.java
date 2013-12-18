package de.isys.jawap.collector.jvm;

public interface ThreadPoolMetricsCollector {
	int getMaxPoolSize();

	int getThreadPoolSize();

	int getThreadPoolNumActiveThreads();

	Integer getThreadPoolNumTasksPending();
}
