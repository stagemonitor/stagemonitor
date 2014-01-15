package de.isys.jawap.collector.jvm;

public interface PooledResource {
	String getName();

	int getMaxPoolSize();

	int getThreadPoolSize();

	int getThreadPoolNumActiveThreads();

	Integer getThreadPoolNumTasksPending();
}
