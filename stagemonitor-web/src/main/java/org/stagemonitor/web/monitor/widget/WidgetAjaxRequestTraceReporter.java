package org.stagemonitor.web.monitor.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.web.monitor.HttpRequestTrace;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class WidgetAjaxRequestTraceReporter extends SpanReporter {

	public static final String CONNECTION_ID = "x-stagemonitor-connection-id";
	private static final long MAX_REQUEST_TRACE_BUFFERING_TIME = 60 * 1000;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private ConcurrentMap<String, ConcurrentLinkedQueue<HttpRequestTrace>> connectionIdToRequestTracesMap =
			new ConcurrentHashMap<String, ConcurrentLinkedQueue<HttpRequestTrace>>();
	private ConcurrentMap<String, Object> connectionIdToLockMap = new ConcurrentHashMap<String, Object>();

	/**
	 * see {@link OldRequestTraceRemover}
	 */
	private ScheduledExecutorService oldRequestTracesRemoverPool;

	public WidgetAjaxRequestTraceReporter() {
	}

	public void init() {
		oldRequestTracesRemoverPool = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				thread.setName("request-trace-remover");
				return thread;
			}
		});

		oldRequestTracesRemoverPool.scheduleAtFixedRate(new OldRequestTraceRemover(),
				MAX_REQUEST_TRACE_BUFFERING_TIME, MAX_REQUEST_TRACE_BUFFERING_TIME, TimeUnit.MILLISECONDS);
	}

	Collection<HttpRequestTrace> getRequestTraces(String connectionId, long requestTimeout) throws IOException {
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			final ConcurrentLinkedQueue<HttpRequestTrace> traces = connectionIdToRequestTracesMap.remove(connectionId);
			if (traces != null) {
				logger.debug("picking up buffered requests");
				return traces;
			} else {
				return blockingWaitForRequestTrace(connectionId, requestTimeout);
			}
		} else {
			throw new IllegalArgumentException("connectionId is empty");
		}
	}

	private Collection<HttpRequestTrace> blockingWaitForRequestTrace(String connectionId, Long requestTimeout) throws IOException {
		Object lock = new Object();
		synchronized (lock) {
			connectionIdToLockMap.put(connectionId, lock);
			try {
				lock.wait(requestTimeout);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				connectionIdToLockMap.remove(connectionId, lock);
			}
			return connectionIdToRequestTracesMap.remove(connectionId);
		}
	}

	@Override
	public void report(ReportArguments reportArguments) throws IOException {
		if (isActive(new IsActiveArguments(reportArguments.getRequestTrace(), reportArguments.getSpan())) && reportArguments.getRequestTrace() instanceof HttpRequestTrace) {
			HttpRequestTrace httpRequestTrace = (HttpRequestTrace) reportArguments.getRequestTrace();

			final String connectionId = httpRequestTrace.getConnectionId();
			if (connectionId != null && !connectionId.trim().isEmpty()) {
				logger.debug("report {} ({})", reportArguments.getRequestTrace().getName(), reportArguments.getRequestTrace().getTimestamp());
				bufferRequestTrace(connectionId, httpRequestTrace);

				final Object lock = connectionIdToLockMap.remove(connectionId);
				if (lock != null) {
					synchronized (lock) {
						lock.notifyAll();
					}
				}
			}
		}
	}

	private void bufferRequestTrace(String connectionId, HttpRequestTrace requestTrace) {
		logger.debug("bufferRequestTrace {} ({})", requestTrace.getName(), requestTrace.getTimestamp());
		ConcurrentLinkedQueue<HttpRequestTrace> httpRequestTraces = new ConcurrentLinkedQueue<HttpRequestTrace>();
		httpRequestTraces.add(requestTrace);

		final ConcurrentLinkedQueue<HttpRequestTrace> alreadyAssociatedValue = connectionIdToRequestTracesMap
				.putIfAbsent(connectionId, httpRequestTraces);
		if (alreadyAssociatedValue != null) {
			alreadyAssociatedValue.add(requestTrace);
		}
	}


	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		if (isActiveArguments.getRequestTrace() instanceof HttpRequestTrace) {
			return ((HttpRequestTrace) isActiveArguments.getRequestTrace()).isShowWidgetAllowed();
		} else {
			logger.warn("RequestTrace is not instanceof HttpRequestTrace: {}", isActiveArguments.getRequestTrace());
			return false;
		}
	}

	/**
	 * Clears old request traces that are buffered in {@link #connectionIdToRequestTracesMap} but are never picked up
	 * to prevent a memory leak
	 */
	private class OldRequestTraceRemover implements Runnable {
		@Override
		public void run() {
			for (Map.Entry<String, ConcurrentLinkedQueue<HttpRequestTrace>> entry : connectionIdToRequestTracesMap.entrySet()) {
				final ConcurrentLinkedQueue<HttpRequestTrace> httpRequestTraces = entry.getValue();
				removeOldRequestTraces(httpRequestTraces);
				if (httpRequestTraces.isEmpty()) {
					removeOrphanEntry(entry);
				}
			}
		}

		private void removeOldRequestTraces(ConcurrentLinkedQueue<HttpRequestTrace> httpRequestTraces) {
			for (Iterator<HttpRequestTrace> iterator = httpRequestTraces.iterator(); iterator.hasNext(); ) {
				HttpRequestTrace httpRequestTrace = iterator.next();
				final long timeInBuffer = System.currentTimeMillis() - httpRequestTrace.getTimestampEnd();
				if (timeInBuffer > MAX_REQUEST_TRACE_BUFFERING_TIME) {
					iterator.remove();
				}
			}
		}

		private void removeOrphanEntry(Map.Entry<String, ConcurrentLinkedQueue<HttpRequestTrace>> entry) {
			// to eliminate race conditions remove only if queue is still empty
			connectionIdToRequestTracesMap.remove(entry.getKey(), new ConcurrentLinkedQueue<HttpRequestTrace>());
		}
	}

	public void close() {
		oldRequestTracesRemoverPool.shutdown();
	}
}
