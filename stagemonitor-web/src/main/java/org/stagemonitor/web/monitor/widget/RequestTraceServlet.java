package org.stagemonitor.web.monitor.widget;

import org.stagemonitor.core.StageMonitor;
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
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebServlet(urlPatterns = "/stagemonitor/request-traces", asyncSupported = true)
public class RequestTraceServlet extends HttpServlet implements RequestTraceReporter {

	public static final String CONNECTION_ID = "x-stagemonitor-connection-id";
	private static final long ASYNC_TIMEOUT = TimeUnit.SECONDS.toMillis(25);
	private static final long MAX_REQUEST_TRACE_BUFFERING_TIME = 60 * 1000;

	private final WebPlugin webPlugin;

	private ConcurrentMap<String, ConcurrentLinkedQueue<HttpRequestTrace>> connectionIdToRequestTracesMap =
			new ConcurrentHashMap<String, ConcurrentLinkedQueue<HttpRequestTrace>>();

	private ConcurrentMap<String, AsyncContext> connectionIdToAsyncContextMap =
			new ConcurrentHashMap<String, AsyncContext>();

	/*
	 * Clears old request traces that are buffered in connectionIdToRequestTracesMap to prevent a memory leak
	 * if the client never picks up the request traces
	 */
	private ScheduledExecutorService oldRequestTracesRemoverPool = Executors.newScheduledThreadPool(1);


	public RequestTraceServlet() {
		this(StageMonitor.getConfiguration(WebPlugin.class));
	}

	public RequestTraceServlet(WebPlugin webPlugin) {
		RequestMonitor.addRequestTraceReporter(this);
		this.webPlugin = webPlugin;
		oldRequestTracesRemoverPool.schedule(new Runnable() {
			@Override
			public void run() {
				for (ConcurrentLinkedQueue<HttpRequestTrace> httpRequestTraces : connectionIdToRequestTracesMap.values()) {
					for (Iterator<HttpRequestTrace> iterator = httpRequestTraces.iterator(); iterator.hasNext(); ) {
						HttpRequestTrace httpRequestTrace = iterator.next();
						final long timeInBuffer = System.currentTimeMillis() - httpRequestTrace.getTimestampEnd();
						if (timeInBuffer > MAX_REQUEST_TRACE_BUFFERING_TIME) {
							iterator.remove();
						}
					}
				}
			}
		}, MAX_REQUEST_TRACE_BUFFERING_TIME, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!webPlugin.isWidgetEnabled()) {
			resp.sendError(404);
			return;
		}
		final String connectionId = req.getParameter("connectionId");
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			if (connectionIdToRequestTracesMap.containsKey(connectionId)) {
				writeRequestTracesToResponse(resp, connectionIdToRequestTracesMap.remove(connectionId));
			} else {
				startAsync(connectionId, req, resp);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private void startAsync(final String connectionId, HttpServletRequest req, HttpServletResponse response) {
		req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", Boolean.TRUE);
		final AsyncContext asyncContext = req.startAsync(req, response);
		asyncContext.addListener(new AsyncListener() {
			@Override
			public void onComplete(AsyncEvent event) throws IOException {
				connectionIdToAsyncContextMap.remove(connectionId);
			}

			@Override
			public void onTimeout(AsyncEvent event) throws IOException {
				final HttpServletResponse response = (HttpServletResponse) event.getSuppliedResponse();
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				response.flushBuffer();
				connectionIdToAsyncContextMap.remove(connectionId);
			}

			@Override
			public void onError(AsyncEvent event) throws IOException {
				onTimeout(event);
			}

			@Override
			public void onStartAsync(AsyncEvent event) throws IOException {
			}
		});
		asyncContext.setTimeout(ASYNC_TIMEOUT);
		connectionIdToAsyncContextMap.put(connectionId, asyncContext);
	}

	@Override
	public <T extends RequestTrace> void reportRequestTrace(T requestTrace) throws IOException {
		if (isActive() && requestTrace instanceof HttpRequestTrace) {
			HttpRequestTrace httpRequestTrace = (HttpRequestTrace) requestTrace;

			final String connectionId = httpRequestTrace.getConnectionId();
			if (connectionId != null && !connectionId.trim().isEmpty()) {
				final AsyncContext asyncContext = connectionIdToAsyncContextMap.remove(connectionId);
				if (asyncContext != null) {

					writeRequestTracesToResponse((HttpServletResponse) asyncContext.getResponse(), getAllRequestTraces(httpRequestTrace, connectionId));
					asyncContext.complete();
				} else {
					bufferRequestTrace(connectionId, httpRequestTrace);
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
			jsonResponse.add(requestTrace.toJson());
		}
		response.getWriter().append(jsonResponse.toString());
	}

	@Override
	public boolean isActive() {
		return webPlugin.isWidgetEnabled();
	}
}
