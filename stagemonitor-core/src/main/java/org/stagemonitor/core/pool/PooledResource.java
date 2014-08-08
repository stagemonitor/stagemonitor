package org.stagemonitor.core.pool;

public interface PooledResource {
	String getName();

	int getMaxPoolSize();

	int getThreadPoolSize();

	int getThreadPoolNumActiveThreads();

	Integer getThreadPoolNumTasksPending();
}
