package org.stagemonitor.web.monitor.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.web.monitor.HttpRequestTrace;

public class RequestTraceServlet extends HttpServlet {

	private static final long DEFAULT_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(25);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final RequestMonitor requestMonitor;
	private final long requestTimeout;
	private WidgetAjaxRequestTraceReporter widgetAjaxRequestTraceReporter;

	public RequestTraceServlet() {
		this(Stagemonitor.getConfiguration(), new WidgetAjaxRequestTraceReporter(), DEFAULT_REQUEST_TIMEOUT);
	}

	public RequestTraceServlet(Configuration configuration, WidgetAjaxRequestTraceReporter reporter, long requestTimeout) {
		this.widgetAjaxRequestTraceReporter = reporter;
		this.requestTimeout = requestTimeout;
		this.requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
	}

	@Override
	public void init() {
		requestMonitor.addReporter(widgetAjaxRequestTraceReporter);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String connectionId = req.getParameter("connectionId");
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			writeRequestTracesToResponse(resp, widgetAjaxRequestTraceReporter.getRequestTraces(connectionId, requestTimeout));
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private void writeRequestTracesToResponse(HttpServletResponse response, Collection<HttpRequestTrace> requestTraces)
			throws IOException {
		if (requestTraces == null) {
			requestTraces = Collections.emptyList();
		}
		response.setContentType("application/json");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "max-age=0, no-cache, no-store, must-revalidate");
		response.setHeader("Expires", "0");
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
	public void destroy() {
		widgetAjaxRequestTraceReporter.close();
	}
}
