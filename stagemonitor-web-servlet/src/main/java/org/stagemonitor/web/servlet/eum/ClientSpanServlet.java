package org.stagemonitor.web.servlet.eum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.web.servlet.MonitoredHttpRequest;
import org.stagemonitor.web.servlet.ServletPlugin;
import org.stagemonitor.web.servlet.useragent.UserAgentParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.stagemonitor.tracing.B3IdentifierTagger.SPAN_ID;
import static org.stagemonitor.tracing.B3IdentifierTagger.TRACE_ID;
import static org.stagemonitor.web.servlet.eum.ClientSpanTagProcessor.TYPE_ALL;
import static org.stagemonitor.web.servlet.eum.WeaselClientSpanExtension.METADATA_BACKEND_SPAN_ID;
import static org.stagemonitor.web.servlet.eum.WeaselClientSpanExtension.METADATA_BACKEND_SPAN_SAMPLING_FLAG;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_APP_CACHE_LOOKUP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_DNS_LOOKUP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_LOAD;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_PROCESSING;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_REDIRECT;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_REQUEST;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_RESPONSE;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_TCP;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_TIME_TO_FIRST_PAINT;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.TIMING_UNLOAD;
import static org.stagemonitor.web.servlet.eum.WeaselSpanTags.getWeaselRequestParameterName;

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
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final Logger logger = LoggerFactory.getLogger(ClientSpanServlet.class);
	private static final String SAMPLED_FLAG = "sp";
	private static final String BACKEND_TRACE_ID = "bt";

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
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_UNLOAD));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_REDIRECT));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_APP_CACHE_LOOKUP));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_DNS_LOOKUP));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_TCP));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_REQUEST));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_RESPONSE));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_PROCESSING));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_LOAD));
		addTagProcessor(durationProcessor(TYPE_PAGE_LOAD, TIMING_TIME_TO_FIRST_PAINT));
		addTagProcessor(new ResourceTimingProcessor());

		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_ERROR, "exception.stack_trace", "st"));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_ERROR, "exception.message", "e"));

		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_XHR, Tags.HTTP_STATUS.getKey(), "st"));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_XHR, "method", "m"));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_XHR, "xhr.requested_url", "u"));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_XHR, "xhr.requested_from", "l"));
		addTagProcessor(new ClientSpanBooleanTagProcessor(TYPE_XHR, "xhr.async", "a"));
		addTagProcessor(durationProcessor(TYPE_XHR, "duration_ms", "d"));
		// The OT-API does not allow to set the SpanContext for the current span, so just set the ids as tags
		// TODO change to SELF reference when https://github.com/opentracing/opentracing-java/pull/212 is merged
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_ALL, SPAN_ID, "s", false));
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_ALL, TRACE_ID, "t", false));
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_ALL, Tags.SAMPLING_PRIORITY.getKey(), SAMPLED_FLAG)
				.lowerBound(0)
				.upperBound(1));
		// sets the same sampling decision as the backend trace (does not work for standalone EUM servers)
		addTagProcessor(new ClientSpanLongTagProcessor(TYPE_PAGE_LOAD, Tags.SAMPLING_PRIORITY.getKey(), METADATA_BACKEND_SPAN_SAMPLING_FLAG)
				.lowerBound(0)
				.upperBound(1));
		// bt = backend trace id
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_PAGE_LOAD, TRACE_ID, BACKEND_TRACE_ID, false));
		addTagProcessor(new ClientSpanTagProcessor(TYPE_PAGE_LOAD, Arrays.asList(BACKEND_TRACE_ID, METADATA_BACKEND_SPAN_ID)) {
			@Override
			protected void processSpanImpl(Span span, Map<String, String[]> requestParameters) {
				final B3HeaderFormat.B3Identifiers backendSpanIds = B3HeaderFormat.B3Identifiers.builder()
						.traceId(getParameterValueOrNull(BACKEND_TRACE_ID, requestParameters))
						.spanId(getParameterValueOrNull(METADATA_BACKEND_SPAN_ID, requestParameters))
						.build();
				final String pageloadSpanId = B3HeaderFormat.getB3Identifiers(tracingPlugin.getTracer(), span).getSpanId();
				// adds the spanId of the pageload span to the parentId of the backend span
				final B3HeaderFormat.B3Identifiers newBackendSpanIds = new B3HeaderFormat.B3Identifiers(backendSpanIds.getTraceId(), backendSpanIds.getSpanId(), pageloadSpanId);
				tracingPlugin.getReportingSpanEventListener().update(backendSpanIds, newBackendSpanIds, Collections.<String, Object>emptyMap());
			}
		});
	}

	private ClientSpanTagProcessor durationProcessor(String typePageLoad, String tagName) {
		return durationProcessor(typePageLoad, tagName, getWeaselRequestParameterName(tagName));
	}

	private ClientSpanLongTagProcessor durationProcessor(String beaconType, String tagName, String requestParameterName) {
		return new ClientSpanLongTagProcessor(beaconType, tagName, requestParameterName)
				.lowerBound(0)
				.upperBound(TimeUnit.MINUTES.toMillis(10))
				.discardSpanOnBoundViolation(true);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleRequest(req, resp);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		setCorsHeaders(resp);
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (servletPlugin.isClientSpanCollectionEnabled()) {
			try {
				convertWeaselBeaconToSpan(req);
			} catch (Exception e) {
				// e.g. non numeric timing values
				logger.info("error handling client span beacon: {}", e.getMessage());
			}
			// always respond with success status code
			setCorsHeaders(resp);
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void setCorsHeaders(HttpServletResponse resp) {
		resp.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		resp.setHeader(ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, OPTIONS");
	}

	private void addTagProcessor(ClientSpanTagProcessor tagProcessor) {
		tagProcessors.add(tagProcessor);
	}

	void convertWeaselBeaconToSpan(HttpServletRequest httpServletRequest) {
		final Map<String, String[]> servletParameters = httpServletRequest.getParameterMap();
		final long durationInMillis;
		if (httpServletRequest.getParameter(PARAMETER_REFERENCE_TIMESTAMP) == null) {
			// in case of error beacons no duration is existent, therefore start equals end
			durationInMillis = 0;
		} else {
			durationInMillis = Long.valueOf(httpServletRequest.getParameter(PARAMETER_DURATION));
		}

		// use server timestamp to deal with wrong user clocks.
		// this allows better insight into the time component (when are response times high)
		// approximate real start by subtracting the client duration from the current server time stamp
		long startInMillis = System.currentTimeMillis() - durationInMillis;
		long finishInMillis = startInMillis + durationInMillis;
		final SpanBuilder spanBuilder = tracingPlugin.getTracer()
				.buildSpan(getOperationName(httpServletRequest))
				.withStartTimestamp(MILLISECONDS.toMicros(startInMillis))
				.withTag(Tags.HTTP_URL.getKey(), getHttpUrl(httpServletRequest))
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

		for (ClientSpanTagProcessor tagProcessor : tagProcessors) {
			tagProcessor.processSpanBuilder(spanBuilder, servletParameters);
		}

		final Span span = spanBuilder.startManual();
		if (tracingPlugin.isSampled(span)) {
			for (ClientSpanTagProcessor tagProcessor : tagProcessors) {
				tagProcessor.processSpan(span, servletParameters);
			}
		}

		if (tracingPlugin.isSampled(span)) {
			if (servletPlugin.isParseUserAgent()) {
				if (userAgentParser == null) {
					userAgentParser = new UserAgentParser();
				}
				userAgentParser.setUserAgentInformation(span, httpServletRequest.getHeader("user-agent"));
			}
			SpanUtils.setClientIp(span, MonitoredHttpRequest.getClientIp(httpServletRequest));
		}

		span.finish(MILLISECONDS.toMicros(finishInMillis));

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
		if (httpServletRequest.getParameter(PARAMETER_URL) != null) {
			operationName = httpServletRequest.getParameter(PARAMETER_URL);
		} else {
			operationName = httpServletRequest.getParameter(PARAMETER_LOCATION);
		}
		return operationName;
	}

}
