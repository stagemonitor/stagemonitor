package org.stagemonitor.requestmonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RequestLogger {

	private static final Logger logger = LoggerFactory.getLogger(RequestLogger.class);

	private ExecutorService asyncLoggPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("async-request-logger");
			return thread;
		}
	});

	public void logStats(final RequestTrace requestTrace) {
		if (logger.isInfoEnabled()) {
			asyncLoggPool.execute(new Runnable() {
				@Override
				public void run() {
					long start = System.currentTimeMillis();
					StringBuilder log = new StringBuilder(10000);
					log.append("\n########## PerformanceStats ##########\n");
					log.append(requestTrace.toString());

					log.append("Printing stats took ").append(System.currentTimeMillis() - start).append(" ms\n");
					log.append("######################################\n\n\n");

					logger.info(log.toString());
				}
			});
		}
	}

}