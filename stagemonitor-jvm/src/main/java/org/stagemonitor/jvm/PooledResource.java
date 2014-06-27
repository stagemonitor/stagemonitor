package org.stagemonitor.jvm;

public interface PooledResource {
	String getName();

	int getMaxPoolSize();

	int getThreadPoolSize();

	int getThreadPoolNumActiveThreads();

	Integer getThreadPoolNumTasksPending();
}
