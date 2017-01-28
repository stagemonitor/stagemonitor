package org.stagemonitor.web.monitor.widget;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.util.Pair;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.reporter.SpanReporter;
import org.stagemonitor.web.monitor.MonitoredHttpRequest;

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

import io.opentracing.Span;

public class WidgetAjaxRequestTraceReporter extends SpanReporter {

	public static final String CONNECTION_ID = "x-stagemonitor-connection-id";
	private static final long MAX_REQUEST_TRACE_BUFFERING_TIME = 60 * 1000;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private ConcurrentMap<String, ConcurrentLinkedQueue<Pair<Long, Span>>> connectionIdToSpanMap =
			new ConcurrentHashMap<String, ConcurrentLinkedQueue<Pair<Long, Span>>>();
	private ConcurrentMap<String, Object> connectionIdToLockMap = new ConcurrentHashMap<String, Object>();

	/**
	 * see {@link OldSpanRemover}
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

		oldRequestTracesRemoverPool.scheduleAtFixedRate(new OldSpanRemover(),
				MAX_REQUEST_TRACE_BUFFERING_TIME, MAX_REQUEST_TRACE_BUFFERING_TIME, TimeUnit.MILLISECONDS);
	}

	Collection<Pair<Long, Span>> getSpans(String connectionId, long requestTimeout) throws IOException {
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			final ConcurrentLinkedQueue<Pair<Long, Span>> traces = connectionIdToSpanMap.remove(connectionId);
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

	private ConcurrentLinkedQueue<Pair<Long, Span>> blockingWaitForRequestTrace(String connectionId, Long requestTimeout) throws IOException {
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
			return connectionIdToSpanMap.remove(connectionId);
		}
	}

	@Override
	public void report(RequestMonitor.RequestInformation requestInformation) throws IOException {
		if (isActive(RequestMonitor.RequestInformation.of(requestInformation.getSpan(), null, requestInformation.getRequestAttributes()))) {

			final String connectionId = (String) requestInformation.getRequestAttributes().get(MonitoredHttpRequest.CONNECTION_ID_ATTRIBUTE);
			if (connectionId != null && !connectionId.trim().isEmpty()) {
				logger.debug("report {}", requestInformation.getSpan());
				bufferRequestTrace(connectionId, requestInformation.getSpan());

				final Object lock = connectionIdToLockMap.remove(connectionId);
				if (lock != null) {
					synchronized (lock) {
						lock.notifyAll();
					}
				}
			}
		}
	}

	private void bufferRequestTrace(String connectionId, Span span) {
		logger.debug("bufferRequestTrace {}", span);
		ConcurrentLinkedQueue<Pair<Long, Span>> httpRequestTraces = new ConcurrentLinkedQueue<Pair<Long, Span>>();
		httpRequestTraces.add(Pair.of(System.currentTimeMillis(), span));

		final ConcurrentLinkedQueue<Pair<Long, Span>> alreadyAssociatedValue = connectionIdToSpanMap
				.putIfAbsent(connectionId, httpRequestTraces);
		if (alreadyAssociatedValue != null) {
			alreadyAssociatedValue.add(Pair.of(System.currentTimeMillis(), span));
		}
	}


	@Override
	public boolean isActive(RequestMonitor.RequestInformation requestInformation) {
		return Boolean.TRUE.equals(requestInformation.getRequestAttributes().get(MonitoredHttpRequest.WIDGET_ALLOWED_ATTRIBUTE));
	}

	/**
	 * Clears old request traces that are buffered in {@link #connectionIdToSpanMap} but are never picked up
	 * to prevent a memory leak
	 */
	private class OldSpanRemover implements Runnable {
		@Override
		public void run() {
			for (Map.Entry<String, ConcurrentLinkedQueue<Pair<Long, Span>>> entry : connectionIdToSpanMap.entrySet()) {
				final ConcurrentLinkedQueue<Pair<Long, Span>> httpSpans = entry.getValue();
				removeOldRequestTraces(httpSpans);
				if (httpSpans.isEmpty()) {
					removeOrphanEntry(entry);
				}
			}
		}

		private void removeOldRequestTraces(ConcurrentLinkedQueue<Pair<Long, Span>> httpRequestTraces) {
			for (Iterator<Pair<Long, Span>> iterator = httpRequestTraces.iterator(); iterator.hasNext(); ) {
				Pair<Long, Span> httpSpan = iterator.next();
				final long timeInBuffer = System.currentTimeMillis() - httpSpan.getA();
				if (timeInBuffer > MAX_REQUEST_TRACE_BUFFERING_TIME) {
					iterator.remove();
				}
			}
		}

		private void removeOrphanEntry(Map.Entry<String, ConcurrentLinkedQueue<Pair<Long, Span>>> entry) {
			// to eliminate race conditions remove only if queue is still empty
			connectionIdToSpanMap.remove(entry.getKey(), new ConcurrentLinkedQueue<Span>());
		}
	}

	public void close() {
		oldRequestTracesRemoverPool.shutdown();
	}
}
