package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.useragent.UserAgentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClientSpanServlet extends HttpServlet {

	static final String PARAMETER_TYPE = "ty";
	static final String TYPE_PAGE_LOAD = "pl";
	private static final String PARAMETER_TIME_STAMP = "ts";
	private static final String PARAMETER_REFERENCE_TIMESTAMP = "r";
	private static final String PARAMETER_DURATION = "d";
	private static final String PARAMETER_URL = "u";
	private static final String PARAMETER_LOCATION = "l";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

	private final TracingPlugin tracingPlugin;
	private final List<ClientSpanTagProcessor> tagProcessors;
	private final UserAgentParser userAgentParser;
	private final ServletPlugin webPlugin;

	public ClientSpanServlet() {
		this(Stagemonitor.getPlugin(TracingPlugin.class), Stagemonitor.getPlugin(ServletPlugin.class));
	}

	ClientSpanServlet(TracingPlugin tracingPlugin, ServletPlugin webPlugin) {
		this.tracingPlugin = tracingPlugin;
		this.webPlugin = webPlugin;
		userAgentParser = new UserAgentParser();
		tagProcessors = new ArrayList<ClientSpanTagProcessor>();
		initializeDefaultTagProcessors();
	}

	private void initializeDefaultTagProcessors() {
		addTagProcessor(new ClientSpanTypeTagProcessor());
		addTagProcessor(new ClientSpanMetadataTagProcessor());

		// have a look at the weasel source [0] and the w3c spec for window.performance.timing [1]
		// [0]: https://github.com/instana/weasel/blob/master/lib/timings.js
		// [1]: https://w3c.github.io/navigation-timing/#processing-model
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.unload", "t_unl"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.redirect", "t_red"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.app_cache_lookup", "t_apc"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.dns_lookup", "t_dns"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.tcp", "t_tcp"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.time_to_first_byte", "t_req"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.response", "t_rsp"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.processing", "t_pro"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.load", "t_loa"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.time_to_first_paint", "t_fp"));
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(req, resp);
	}

	private void handleRequest(HttpServletRequest req, HttpServletResponse resp) {
		if (webPlugin.isClientSpanCollectionEnabled()) {
			convertWeaselTraceToStagemonitorTrace(req);
			resp.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}

	private void addTagProcessor(ClientSpanTagProcessor tagProcessor) {
		tagProcessors.add(tagProcessor);
	}

	void convertWeaselTraceToStagemonitorTrace(HttpServletRequest httpServletRequest) {
		final Map<String, String[]> servletParameters = httpServletRequest.getParameterMap();
		final long startTimeStampInMilliseconds;
		final long possiblyOffsettedStartTimeStampInMilliseconds = Long.parseLong(httpServletRequest.getParameter(PARAMETER_TIME_STAMP));
		final Long durationInMilliseconds;
		final String referenceTimestampParameter = httpServletRequest.getParameter(PARAMETER_REFERENCE_TIMESTAMP);
		if (referenceTimestampParameter == null) {
			startTimeStampInMilliseconds = possiblyOffsettedStartTimeStampInMilliseconds;
			durationInMilliseconds = 0L;
		} else {
			final long referenceTimeStampInMilliseconds = Long.parseLong(referenceTimestampParameter);
			startTimeStampInMilliseconds = possiblyOffsettedStartTimeStampInMilliseconds + referenceTimeStampInMilliseconds;
			final Long durationOffset = Long.valueOf(httpServletRequest.getParameter(PARAMETER_DURATION));
			durationInMilliseconds = durationOffset + referenceTimeStampInMilliseconds;
		}

		final String httpUrl = getHttpUrl(httpServletRequest);
		final String operationName = httpServletRequest.getParameter(PARAMETER_TYPE) + " " + httpUrl;
		final SpanBuilder spanBuilder = tracingPlugin.getTracer().buildSpan(operationName)
				.withStartTimestamp(MILLISECONDS.toMicros(startTimeStampInMilliseconds))
				.withTag("http.url", httpUrl);

		for (ClientSpanTagProcessor tagProcessor : tagProcessors) {
			tagProcessor.processSpanBuilder(spanBuilder, servletParameters);
		}

		final Span span = spanBuilder.start();

		for (ClientSpanTagProcessor tagProcessor : tagProcessors) {
			tagProcessor.processSpan(span, servletParameters);
		}

		if (webPlugin.isParseUserAgent()) {
			userAgentParser.setUserAgentInformation(span, httpServletRequest.getHeader("user-agent"));
		}
		SpanUtils.setClientIp(span, httpServletRequest.getRemoteAddr());

		// TODO: extract backend trace id (if sent) and attach span to that trace id

		span.finish(MILLISECONDS.toMicros(durationInMilliseconds));
	}

	private String getHttpUrl(HttpServletRequest httpServletRequest) {
		String operationName;
		if (httpServletRequest.getParameter(PARAMETER_LOCATION) != null) {
			operationName = httpServletRequest.getParameter(PARAMETER_LOCATION);
		} else {
			operationName = httpServletRequest.getParameter(PARAMETER_URL);
		}
		return operationName;
	}


}
