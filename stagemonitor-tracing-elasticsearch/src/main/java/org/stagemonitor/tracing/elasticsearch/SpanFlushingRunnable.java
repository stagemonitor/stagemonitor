package org.stagemonitor.tracing.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

class SpanFlushingRunnable implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(SpanFlushingRunnable.class);
	private final Callable<Boolean> flushCallable;

	SpanFlushingRunnable(Callable<Boolean> flushCallable) {
		this.flushCallable = flushCallable;
	}

	@Override
	public synchronized void run() {
		try {
			boolean hasMoreElements = true;
			while (hasMoreElements) {
				hasMoreElements = flushCallable.call();
			}
		} catch (Exception e) {
			logger.warn("Exception while reporting spans to Elasticsearch", e);
		}
	}
}
