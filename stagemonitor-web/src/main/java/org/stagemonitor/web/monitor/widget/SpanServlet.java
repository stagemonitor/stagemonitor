package org.stagemonitor.web.monitor.widget;

import com.uber.jaeger.Span;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SpanServlet extends HttpServlet {

	private static final long DEFAULT_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(25);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final RequestMonitor requestMonitor;
	private final long requestTimeout;
	private WidgetAjaxRequestTraceReporter widgetAjaxRequestTraceReporter;

	public SpanServlet() {
		this(Stagemonitor.getConfiguration(), new WidgetAjaxRequestTraceReporter(), DEFAULT_REQUEST_TIMEOUT);
	}

	public SpanServlet(Configuration configuration, WidgetAjaxRequestTraceReporter reporter, long requestTimeout) {
		this.widgetAjaxRequestTraceReporter = reporter;
		this.requestTimeout = requestTimeout;
		this.requestMonitor = configuration.getConfig(RequestMonitorPlugin.class).getRequestMonitor();
		requestMonitor.addReporter(widgetAjaxRequestTraceReporter);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String connectionId = req.getParameter("connectionId");
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			writeRequestTracesToResponse(resp, widgetAjaxRequestTraceReporter.getSpans(connectionId, requestTimeout));
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private void writeRequestTracesToResponse(HttpServletResponse response, Collection<Span> spans)
			throws IOException {
		if (spans == null) {
			spans = Collections.emptyList();
		}
		response.setContentType("application/json");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "max-age=0, no-cache, no-store, must-revalidate");
		response.setHeader("Expires", "0");
		response.setCharacterEncoding("UTF-8");

		final ArrayList<String> jsonResponse = new ArrayList<String>(spans.size());
		for (Span span : spans) {
			logger.debug("writeRequestTracesToResponse {}", span);
			jsonResponse.add(JsonUtils.toJson(span, SpanUtils.CALL_TREE_ASCII));
		}
		response.getWriter().print(jsonResponse.toString());
		response.getWriter().close();
	}

	@Override
	public void destroy() {
		widgetAjaxRequestTraceReporter.close();
	}
}
