package org.stagemonitor.web.monitor.widget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.Pair;
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

	private final long requestTimeout;
	private final Configuration configuration;
	private WidgetAjaxSpanReporter widgetAjaxSpanReporter;

	public SpanServlet() {
		this(Stagemonitor.getConfiguration(), new WidgetAjaxSpanReporter(), DEFAULT_REQUEST_TIMEOUT);
	}

	public SpanServlet(Configuration configuration, WidgetAjaxSpanReporter reporter, long requestTimeout) {
		this.configuration = configuration;
		this.widgetAjaxSpanReporter = reporter;
		this.requestTimeout = requestTimeout;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String connectionId = req.getParameter("connectionId");
		if (connectionId != null && !connectionId.trim().isEmpty()) {
			writeSpansToResponse(resp, widgetAjaxSpanReporter.getSpans(connectionId, requestTimeout));
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private void writeSpansToResponse(HttpServletResponse response, Collection<Pair<Long, io.opentracing.Span>> spans)
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
		for (Pair<Long, io.opentracing.Span> spanPair : spans) {
			logger.debug("writeSpansToResponse {}", spanPair);
			jsonResponse.add(JsonUtils.toJson(spanPair.getB(), SpanUtils.CALL_TREE_ASCII));
		}
		response.getWriter().print(jsonResponse.toString());
		response.getWriter().close();
	}

	@Override
	public void destroy() {
		widgetAjaxSpanReporter.close();
	}

	public void onStagemonitorStarted() {
		configuration.getConfig(RequestMonitorPlugin.class).addReporter(widgetAjaxSpanReporter);
	}
}
