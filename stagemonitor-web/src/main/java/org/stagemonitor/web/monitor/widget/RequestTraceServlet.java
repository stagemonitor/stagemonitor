package org.stagemonitor.web.monitor.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestTrace;
import org.stagemonitor.requestmonitor.RequestTraceReporter;
import org.stagemonitor.web.WebPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import java.util.concurrent.TimeUnit;

@WebServlet(urlPatterns = "/stagemonitor/request-traces", asyncSupported = true)
public class RequestTraceServlet extends HttpServlet implements RequestTraceReporter {

	public static final String CONNECTION_ID = "x-stagemonitor-connection-id";
	private static final long REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(25);
	private static final long MAX_REQUEST_TRACE_BUFFERING_TIME = 60 * 1000;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final WebPlugin webPlugin;
	private ConcurrentMap<String, ConcurrentLinkedQueue<HttpRequestTrace>> connectionIdToRequestTracesMap =
			new ConcurrentHashMap<String, ConcurrentLinkedQueue<HttpRequestTrace>>();
	private ConcurrentMap<String, AsyncContext> connectionIdToAsyncContextMap =
			new ConcurrentHashMap<String, AsyncContext>();
	private ConcurrentMap<String, Object> connectionIdToLockMap =
			new ConcurrentHashMap<String, Object>();

	/**
	 * see {@link OldRequestTraceRemover}
	 */
	private ScheduledExecutorService oldRequestTracesRemoverPool = Executors.newScheduledThreadPool(1);


	public RequestTraceServlet() {
		this(Stagemonitor.getConfiguration(WebPlugin.class));
	}

	public RequestTraceServlet(WebPlugin webPlugin) {
		RequestMonitor.addRequestTraceReporter(this);
		this.webPlugin = webPlugin;
		oldRequestTracesRemoverPool.schedule(new OldRequestTraceRemover(), MAX_REQUEST_TRACE_BUFFERING_TIME, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!webPlugin.isWidgetEnabled()) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		final String connectionId = req.getParameter("connectionId");
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			if (connectionIdToRequestTracesMap.containsKey(connectionId)) {
				logger.debug("picking up buffered requests");
				writeRequestTracesToResponse(resp, connectionIdToRequestTracesMap.remove(connectionId));
			} else {
				if (!startAsync(connectionId, req, resp)) {
					blockingWaitForRequestTrace(connectionId, resp);
				}
			}
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private void blockingWaitForRequestTrace(String connectionId, HttpServletResponse resp) throws IOException {
		logger.warn("Request does not support async processing. Falling back to blocking. Please mark your filters with <async-supported>true</async-supported>.");
		Object lock = new Object();
		synchronized (lock) {
			if (connectionIdToLockMap.putIfAbsent(connectionId, lock) == null) {
				try {
					lock.wait(REQUEST_TIMEOUT);
					connectionIdToLockMap.remove(connectionId);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if (connectionIdToRequestTracesMap.containsKey(connectionId)) {
					writeRequestTracesToResponse(resp, connectionIdToRequestTracesMap.remove(connectionId));
				} else {
					writeEmptyResponse(resp);
				}
			}
		}
	}

	private boolean startAsync(final String connectionId, HttpServletRequest req, HttpServletResponse response) {
		req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", Boolean.TRUE);
		if (req.isAsyncSupported()) {
			final AsyncContext asyncContext = req.startAsync(req, response);
			asyncContext.addListener(new AsyncListener() {
				@Override
				public void onComplete(AsyncEvent event) throws IOException {
				}

				@Override
				public void onTimeout(AsyncEvent event) throws IOException {
					connectionIdToAsyncContextMap.remove(connectionId, event.getAsyncContext());
					writeEmptyResponse((HttpServletResponse)event.getSuppliedResponse());
				}

				@Override
				public void onError(AsyncEvent event) throws IOException {
					onTimeout(event);
				}

				@Override
				public void onStartAsync(AsyncEvent event) throws IOException {
				}
			});
			asyncContext.setTimeout(REQUEST_TIMEOUT);
			connectionIdToAsyncContextMap.put(connectionId, asyncContext);
		}
		return req.isAsyncSupported();
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) throws IOException {
		if (isActive() && requestTrace instanceof HttpRequestTrace) {
			HttpRequestTrace httpRequestTrace = (HttpRequestTrace) requestTrace;

			final String connectionId = httpRequestTrace.getConnectionId();
			if (connectionId != null && !connectionId.trim().isEmpty()) {
				logger.debug("reportRequestTrace {} ({})", requestTrace.getName(), requestTrace.getTimestamp());
				final AsyncContext asyncContext = connectionIdToAsyncContextMap.remove(connectionId);
				if (asyncContext != null && !asyncContext.getResponse().isCommitted()) {
					logger.debug("asyncContext {}", httpRequestTrace.getConnectionId());
					writeRequestTracesToResponse((HttpServletResponse) asyncContext.getResponse(), getAllRequestTraces(httpRequestTrace, connectionId));
					asyncContext.complete();
				} else {
					bufferRequestTrace(connectionId, httpRequestTrace);
				}

				final Object lock = connectionIdToLockMap.remove(connectionId);
				if (lock != null) {
					synchronized (lock) {
						lock.notifyAll();
					}
				}
			}
		}
	}

	private Collection<HttpRequestTrace> getAllRequestTraces(HttpRequestTrace httpRequestTrace, String connectionId) {
		Collection<HttpRequestTrace> allRequestTraces = new ConcurrentLinkedQueue<HttpRequestTrace>();
		allRequestTraces.add(httpRequestTrace);

		final ConcurrentLinkedQueue<HttpRequestTrace> bufferedRequestTraces = connectionIdToRequestTracesMap.remove(connectionId);
		if (bufferedRequestTraces != null) {
			allRequestTraces.addAll(bufferedRequestTraces);
		}
		return allRequestTraces;
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
		response.setContentType("application/json");
		response.setHeader("Pragma", "no-cache");
		response.setCharacterEncoding("UTF-8");

		final ArrayList<String> jsonResponse = new ArrayList<String>(requestTraces.size());
		for (HttpRequestTrace requestTrace : requestTraces) {
			logger.debug("writeRequestTracesToResponse {} ({})", requestTrace.getName(), requestTrace.getTimestamp());
			jsonResponse.add(requestTrace.toJson());
		}
		response.getOutputStream().print(jsonResponse.toString());
	}

	private void writeEmptyResponse(HttpServletResponse resp) throws IOException {
		writeRequestTracesToResponse(resp, Collections.<HttpRequestTrace>emptyList());
	}

	@Override
	public boolean isActive() {
		return webPlugin.isWidgetEnabled();
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
}
