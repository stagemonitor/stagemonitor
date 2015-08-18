package org.stagemonitor.web.monitor.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.RequestTraceReporter;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;

public class RequestTraceServlet extends HttpServlet implements RequestTraceReporter {

	public static final String CONNECTION_ID = "x-stagemonitor-connection-id";
	private static final long DEFAULT_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(25);
	private static final long MAX_REQUEST_TRACE_BUFFERING_TIME = 60 * 1000;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final WebPlugin webPlugin;
	private final Configuration configuration;
	private final long requestTimeout;
	private ConcurrentMap<String, ConcurrentLinkedQueue<HttpRequestTrace>> connectionIdToRequestTracesMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<HttpRequestTrace>>();
	private ConcurrentMap<String, Object> connectionIdToLockMap = new ConcurrentHashMap<String, Object>();

	/**
	 * see {@link OldRequestTraceRemover}
	 */
	private ScheduledExecutorService oldRequestTracesRemoverPool = Executors.newScheduledThreadPool(1, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("request-trace-remover");
			return thread;
		}
	});


	public RequestTraceServlet() {
		this(Stagemonitor.getConfiguration(), DEFAULT_REQUEST_TIMEOUT);
	}

	public RequestTraceServlet(Configuration configuration, long requestTimeout) {
		RequestMonitor.addRequestTraceReporter(this);
		this.requestTimeout = requestTimeout;
		this.configuration = configuration;
		this.webPlugin = configuration.getConfig(WebPlugin.class);
		oldRequestTracesRemoverPool.schedule(new OldRequestTraceRemover(), MAX_REQUEST_TRACE_BUFFERING_TIME, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String connectionId = req.getParameter("connectionId");
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			final ConcurrentLinkedQueue<HttpRequestTrace> traces = connectionIdToRequestTracesMap.remove(connectionId);
			if (traces != null) {
				logger.debug("picking up buffered requests");
				writeRequestTracesToResponse(resp, traces);
			} else {
				blockingWaitForRequestTrace(connectionId, resp);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private void blockingWaitForRequestTrace(String connectionId, HttpServletResponse resp) throws IOException {
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
			writeRequestTracesToResponse(resp, connectionIdToRequestTracesMap.remove(connectionId));
		}
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) throws IOException {
		if (isActive(requestTrace) && requestTrace instanceof HttpRequestTrace) {
			HttpRequestTrace httpRequestTrace = (HttpRequestTrace) requestTrace;

			final String connectionId = httpRequestTrace.getConnectionId();
			if (connectionId != null && !connectionId.trim().isEmpty()) {
				logger.debug("reportRequestTrace {} ({})", requestTrace.getName(), requestTrace.getTimestamp());
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

	private void writeRequestTracesToResponse(HttpServletResponse response, Collection<HttpRequestTrace> requestTraces)
			throws IOException {
		if (requestTraces == null) {
			requestTraces = Collections.emptyList();
		}
		response.setContentType("application/json");
		response.setHeader("Pragma", "no-cache");
		response.setCharacterEncoding("UTF-8");

		final ArrayList<String> jsonResponse = new ArrayList<String>(requestTraces.size());
		for (HttpRequestTrace requestTrace : requestTraces) {
			logger.debug("writeRequestTracesToResponse {} ({})", requestTrace.getName(), requestTrace.getTimestamp());
			jsonResponse.add(requestTrace.toJson());
		}
		response.getOutputStream().print(jsonResponse.toString());
		response.getOutputStream().close();
	}

	@Override
	public <T extends RequestTrace> boolean isActive(T requestTrace) {
		if (requestTrace instanceof HttpRequestTrace) {
			final HttpRequestTrace httpRequestTrace = (HttpRequestTrace) requestTrace;
			final boolean active = webPlugin.isWidgetAndStagemonitorEndpointsAllowed(httpRequestTrace.getRequest(), configuration);
			logger.debug("RequestTraceServlet#isActive: {}", active);
			return active;
		} else {
			logger.warn("RequestTrace is not instanceof HttpRequestTrace: {}", requestTrace);
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

	@Override
	public void destroy() {
		oldRequestTracesRemoverPool.shutdown();
	}
}
