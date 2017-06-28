package org.stagemonitor.web.servlet.eum;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.useragent.UserAgentParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClientSpanServlet extends HttpServlet {

	static final String PARAMETER_TYPE = "ty";
	static final String TYPE_PAGE_LOAD = "pl";
	static final String TYPE_ERROR = "err";
	static final String TYPE_XHR = "xhr";
	private static final String PARAMETER_TIME_STAMP = "ts";
	private static final String PARAMETER_REFERENCE_TIMESTAMP = "r";
	private static final String PARAMETER_DURATION = "d";
	private static final String PARAMETER_URL = "u";
	private static final String PARAMETER_LOCATION = "l";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

	private final TracingPlugin tracingPlugin;
	private final List<ClientSpanTagProcessor> tagProcessors;
	private UserAgentParser userAgentParser;
	private final ServletPlugin servletPlugin;

	public ClientSpanServlet() {
		this(Stagemonitor.getPlugin(TracingPlugin.class), Stagemonitor.getPlugin(ServletPlugin.class));
	}

	ClientSpanServlet(TracingPlugin tracingPlugin, ServletPlugin servletPlugin) {
		this.tracingPlugin = tracingPlugin;
		this.servletPlugin = servletPlugin;
		tagProcessors = new ArrayList<ClientSpanTagProcessor>();
		initializeDefaultTagProcessors();
	}

	private void initializeDefaultTagProcessors() {
		addTagProcessor(new ClientSpanTypeTagProcessor());
		addTagProcessor(new ClientSpanMetadataTagProcessor(servletPlugin));

		// have a look at the weasel source [0] and the w3c spec for window.performance.timing [1]
		// [0]: https://github.com/instana/weasel/blob/master/lib/timings.js
		// [1]: https://w3c.github.io/navigation-timing/#processing-model
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.unload", "t_unl"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.redirect", "t_red"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.app_cache_lookup", "t_apc"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.dns_lookup", "t_dns"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.tcp", "t_tcp"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.request", "t_req"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.response", "t_rsp"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.processing", "t_pro"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.load", "t_loa"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, "timing.time_to_first_paint", "t_fp"));

		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_ERROR, "exception.stack_trace", "st"));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_ERROR, "exception.message", "e"));

		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_XHR, "http.status", "st"));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_XHR, "method", "m"));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_XHR, "xhr.requested_url", "u"));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_XHR, "xhr.requested_from", "l"));
		addTagProcessor(new ClientSpanBooleanTagProcessor(TYPE_XHR, "xhr.async", "a"));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_XHR, "duration_ms", "d"));
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
		if (servletPlugin.isClientSpanCollectionEnabled()) {
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
		final Long finishTimestampInMilliseconds;
		final String referenceTimestampParameter = httpServletRequest.getParameter(PARAMETER_REFERENCE_TIMESTAMP);
		if (referenceTimestampParameter == null) {
			// in case of error beacons no duration is existent, therefore start equals end
			startTimeStampInMilliseconds = possiblyOffsettedStartTimeStampInMilliseconds;
			finishTimestampInMilliseconds = possiblyOffsettedStartTimeStampInMilliseconds;
		} else {
			final long referenceTimeStampInMilliseconds = Long.parseLong(referenceTimestampParameter);
			startTimeStampInMilliseconds = possiblyOffsettedStartTimeStampInMilliseconds + referenceTimeStampInMilliseconds;
			final Long durationOffset = Long.valueOf(httpServletRequest.getParameter(PARAMETER_DURATION));
			finishTimestampInMilliseconds = durationOffset + startTimeStampInMilliseconds;
		}

		final SpanBuilder spanBuilder = tracingPlugin.getTracer().buildSpan(getOperationName(httpServletRequest))
				.withStartTimestamp(MILLISECONDS.toMicros(startTimeStampInMilliseconds))
				.withTag(Tags.HTTP_URL.getKey(), getHttpUrl(httpServletRequest))
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

		for (ClientSpanTagProcessor tagProcessor : tagProcessors) {
			tagProcessor.processSpanBuilder(spanBuilder, servletParameters);
		}

		final Span span = spanBuilder.start();

		for (ClientSpanTagProcessor tagProcessor : tagProcessors) {
			tagProcessor.processSpan(span, servletParameters);
		}

		if (servletPlugin.isParseUserAgent()) {
			if (userAgentParser == null) {
				userAgentParser = new UserAgentParser();
			}
			userAgentParser.setUserAgentInformation(span, httpServletRequest.getHeader("user-agent"));
		}
		SpanUtils.setClientIp(span, httpServletRequest.getRemoteAddr());

		// TODO: extract backend trace id (if sent) and attach span to that trace id

		span.finish(MILLISECONDS.toMicros(finishTimestampInMilliseconds));
	}

	private String getOperationName(HttpServletRequest httpServletRequest) {
		String httpUrl = getHttpUrl(httpServletRequest);
		try {
			URL url = new URL(httpUrl);
			return url.getPath();
		} catch (MalformedURLException e) {
			return httpUrl;
		}
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
