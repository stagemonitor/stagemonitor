package org.stagemonitor.web.monitor.widget;

import com.uber.jaeger.Span;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class WidgetAjaxRequestTraceReporter extends SpanReporter {

	public static final String CONNECTION_ID = "x-stagemonitor-connection-id";
	private static final long MAX_REQUEST_TRACE_BUFFERING_TIME = 60 * 1000;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private ConcurrentMap<String, ConcurrentLinkedQueue<Span>> connectionIdToSpanMap =
			new ConcurrentHashMap<String, ConcurrentLinkedQueue<Span>>();
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

	Collection<Span> getSpans(String connectionId, long requestTimeout) throws IOException {
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			final ConcurrentLinkedQueue<Span> traces = connectionIdToSpanMap.remove(connectionId);
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

	private Collection<Span> blockingWaitForRequestTrace(String connectionId, Long requestTimeout) throws IOException {
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
	public void report(ReportArguments reportArguments) throws IOException {
		if (isActive(new IsActiveArguments(reportArguments.getSpan(), reportArguments.getRequestAttributes()))) {
			Span httpSpan = reportArguments.getInternalSpan();

			final String connectionId = (String) reportArguments.getRequestAttributes().get(MonitoredHttpRequest.CONNECTION_ID_ATTRIBUTE);
			if (connectionId != null && !connectionId.trim().isEmpty()) {
				logger.debug("report {}", reportArguments.getSpan());
				bufferRequestTrace(connectionId, httpSpan);

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
		ConcurrentLinkedQueue<Span> httpRequestTraces = new ConcurrentLinkedQueue<Span>();
		httpRequestTraces.add(span);

		final ConcurrentLinkedQueue<Span> alreadyAssociatedValue = connectionIdToSpanMap
				.putIfAbsent(connectionId, httpRequestTraces);
		if (alreadyAssociatedValue != null) {
			alreadyAssociatedValue.add(span);
		}
	}


	@Override
	public boolean isActive(IsActiveArguments isActiveArguments) {
		return Boolean.TRUE.equals(isActiveArguments.getRequestAttributes().get(MonitoredHttpRequest.WIDGET_ALLOWED_ATTRIBUTE));
	}

	/**
	 * Clears old request traces that are buffered in {@link #connectionIdToSpanMap} but are never picked up
	 * to prevent a memory leak
	 */
	private class OldSpanRemover implements Runnable {
		@Override
		public void run() {
			for (Map.Entry<String, ConcurrentLinkedQueue<Span>> entry : connectionIdToSpanMap.entrySet()) {
				final ConcurrentLinkedQueue<Span> httpRequestTraces = entry.getValue();
				removeOldRequestTraces(httpRequestTraces);
				if (httpRequestTraces.isEmpty()) {
					removeOrphanEntry(entry);
				}
			}
		}

		private void removeOldRequestTraces(ConcurrentLinkedQueue<Span> httpRequestTraces) {
			for (Iterator<Span> iterator = httpRequestTraces.iterator(); iterator.hasNext(); ) {
				Span httpSpan = iterator.next();
				final long timeInBuffer = System.currentTimeMillis() - TimeUnit.MICROSECONDS.toMillis(httpSpan.getDuration());
				if (timeInBuffer > MAX_REQUEST_TRACE_BUFFERING_TIME) {
					iterator.remove();
				}
			}
		}

		private void removeOrphanEntry(Map.Entry<String, ConcurrentLinkedQueue<Span>> entry) {
			// to eliminate race conditions remove only if queue is still empty
			connectionIdToSpanMap.remove(entry.getKey(), new ConcurrentLinkedQueue<Span>());
		}
	}

	public void close() {
		oldRequestTracesRemoverPool.shutdown();
	}
}
