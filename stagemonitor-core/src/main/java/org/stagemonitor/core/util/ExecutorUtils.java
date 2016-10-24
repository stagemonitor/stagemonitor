package org.stagemonitor.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.CorePlugin;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ExecutorUtils {

	private static final Logger logger = LoggerFactory.getLogger(ExecutorUtils.class);

	private ExecutorUtils() {
		// don't instantiate
	}

	public static ThreadPoolExecutor createSingleThreadDeamonPool(final String threadName, int queueCapacity) {
		final ThreadFactory daemonThreadFactory = new NamedThreadFactory(threadName);
		return new MyThreadPoolExecutor(queueCapacity, daemonThreadFactory, threadName);
	}

	public static void logRejectionWarning(RejectedExecutionException e) {
		logger.warn("The limit of pending tasks for the executor is reached. " +
				"This could be due to a unreachable service such as elasticsearch or due to a spike in incoming requests. " +
				"Consider increasing the default capacity limit with the configuration key '" + CorePlugin.POOLS_QUEUE_CAPACITY_LIMIT_KEY + "'\n"
				+ e.getMessage());
	}

	public static class NamedThreadFactory implements ThreadFactory {
		private final String threadName;

		public NamedThreadFactory(String threadName) {
			this.threadName = threadName;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName(threadName);
			return thread;
		}
	}

	private static class MyThreadPoolExecutor extends ThreadPoolExecutor {
		private final String threadName;

		public MyThreadPoolExecutor(int queueCapacity, ThreadFactory daemonThreadFactory, String threadName) {
			super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(queueCapacity), daemonThreadFactory);
			this.threadName = threadName;
		}

		@Override
		public String toString() {
			return super.toString() + "(thread name = " + threadName + ")";
		}

		/**
		 * Overriding this method makes sure that exceptions thrown by a task are not silently swallowed.
		 * <p/>
		 * Thanks to nos for this solution: http://stackoverflow.com/a/2248203/1125055
		 */
		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			super.afterExecute(r, t);
			if (t == null && r instanceof Future<?>) {
				try {
					Future<?> future = (Future<?>) r;
					if (future.isDone()) {
						future.get();
					}
				} catch (CancellationException ce) {
					t = ce;
				} catch (ExecutionException ee) {
					t = ee.getCause();
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt(); // ignore/reset
				}
			}
			if (t != null) {
				logger.warn("Error while executing task in " + this, t);
			}
		}
	}
}
