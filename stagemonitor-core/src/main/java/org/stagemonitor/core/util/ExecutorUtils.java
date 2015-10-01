package org.stagemonitor.core.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;

public final class ExecutorUtils {

	private static final Logger logger = LoggerFactory.getLogger(ExecutorUtils.class);

	private ExecutorUtils() {
		// don't instantiate
	}

	public static ThreadPoolExecutor createSingleThreadDeamonPool(final String threadName, int queueCapacity) {
		final ThreadFactory daemonThreadFactory = new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				thread.setName(threadName);
				return thread;
			}
		};
		return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(queueCapacity), daemonThreadFactory) {
			@Override
			public String toString() {
				return super.toString() + "(thread name = " + threadName + ")";
			}
		};
	}

	public static void logRejectionWarning(RejectedExecutionException e) {
		logger.warn("The limit of pending tasks for the executor is reached. " +
				"This could be due to a unreachable service such as elasticsearch or due to a spike in incoming requests. " +
				"Consider increasing the default capacity limit with the configuration key '" + CorePlugin.POOLS_QUEUE_CAPACITY_LIMIT_KEY + "'\n"
				+ e.getMessage());
	}

}
