package org.stagemonitor.core.pool;

import org.stagemonitor.core.metrics.metrics2.MetricName;

/**
 * Represents metrics of a pooled resource like a application server thread pool or a JDBC connection pool
 */
public interface PooledResource {

	/**
	 * Returns the name of the pool.
	 * It will be part of the metric name.
	 *
	 * @return the name of the pool.
	 */
	MetricName getName();

	/**
	 * Returns the maximal size of the pool.
	 * @return the maximal size of the pool.
	 */
	int getMaxPoolSize();

	/**
	 * Returns the actual size of the pool.
	 * It can be lower than or equal to the {@link #getMaxPoolSize()}
	 *
	 * @return the actual size of the pool.
	 */
	int getActualPoolSize();

	/**
	 * Returns the number of resources that are currently in use.
	 * For example the number of JDBC connections that are currently used.
	 *
	 * @return the number of resources that are currently in use.
	 */
	int getPoolNumActive();

	/**
	 * Returns the number of tasks that are waiting for a available resource if all resources are in use or
	 * <code>null</code> if this metric is not available for this type of resource pool.
	 *
	 * @return the number of tasks that are waiting for a available resource if all resources are in use or
	 * <code>null</code> if this metric is not available for this type of resource pool.
	 */
	Integer getNumTasksPending();
}
