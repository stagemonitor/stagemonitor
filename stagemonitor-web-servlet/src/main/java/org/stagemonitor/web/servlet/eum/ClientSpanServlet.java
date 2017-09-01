package org.stagemonitor.web.servlet.eum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.B3HeaderFormat;
import org.stagemonitor.tracing.SpanContextInformation;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.SpanUtils;
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
import static org.stagemonitor.web.servlet.eum.WeaselClientSpanExtension.METADATA_BACKEND_SPAN_ID;
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
		addTagProcessor(new ClientSpanStringTagProcessor(ClientSpanTagProcessor.TYPE_ALL, SPAN_ID, "s", false));
		addTagProcessor(new ClientSpanStringTagProcessor(ClientSpanTagProcessor.TYPE_ALL, TRACE_ID, "t", false));
		// bt = backend trace id
		addTagProcessor(new ClientSpanStringTagProcessor(TYPE_PAGE_LOAD, TRACE_ID, "bt", false));
		addTagProcessor(new ClientSpanTagProcessor(TYPE_PAGE_LOAD, Arrays.asList("bt", METADATA_BACKEND_SPAN_ID)) {
			@Override
			protected void processSpanImpl(Span span, Map<String, String[]> requestParameters) {
				final B3HeaderFormat.B3Identifiers backendSpanIds = B3HeaderFormat.B3Identifiers.builder()
						.traceId(getParameterValueOrNull("bt", requestParameters))
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

		// use local timestamp to deal with wrong user clocks.
		// this allows better insight into the time component (when are high response times high)
		long startInMillis = System.currentTimeMillis();
		final SpanBuilder spanBuilder = tracingPlugin.getTracer()
				.buildSpan(getOperationName(httpServletRequest))
				.withStartTimestamp(MILLISECONDS.toMicros(startInMillis))
				.withTag(Tags.HTTP_URL.getKey(), getHttpUrl(httpServletRequest))
				.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

		for (ClientSpanTagProcessor tagProcessor : tagProcessors) {
			tagProcessor.processSpanBuilder(spanBuilder, servletParameters);
		}

		final Span span = spanBuilder.start();
		if (SpanContextInformation.forSpan(span).isSampled()) {
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
		}

		// TODO: extract backend trace id (if sent) and attach span to that trace id
		span.finish(MILLISECONDS.toMicros(startInMillis + durationInMillis));

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
