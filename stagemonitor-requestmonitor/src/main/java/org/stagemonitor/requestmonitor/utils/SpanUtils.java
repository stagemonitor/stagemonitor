package org.stagemonitor.requestmonitor.utils;

import com.uber.jaeger.utils.Utils;

import org.stagemonitor.core.util.JsonUtils;
import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.profiler.CallStackElement;
import org.stagemonitor.requestmonitor.reporter.ExternalRequestMetricsReporter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class SpanUtils {

	public static final String IPV4_STRING = "peer.ipv4_string";
	public static final String CALL_TREE_ASCII = "call_tree_ascii";
	public static final String CALL_TREE_JSON = "call_tree_json";
	public static final String HTTP_HEADERS_PREFIX = "http.headers.";
	public static final String USERNAME = "username";
	public static final String PARAMETERS_PREFIX = "parameters.";

	private SpanUtils() {
	}

	public static void setClientIp(Span span, String clientIp) {
		try {
			final InetAddress inetAddress = InetAddress.getByName(clientIp);
			if (inetAddress instanceof Inet4Address) {
				Tags.PEER_HOST_IPV4.set(span, clientIp != null ? Utils.ipToInt(clientIp) : null);
				span.setTag(IPV4_STRING, clientIp);
			} else if (inetAddress instanceof Inet6Address) {
				Tags.PEER_HOST_IPV6.set(span, clientIp);
			}
		} catch (UnknownHostException e) {
			// ignore
		}
	}

	public static void setParameters(Span span, Map<String, String> parameters) {
		if (parameters == null) {
			return;
		}
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			span.setTag(PARAMETERS_PREFIX + StringUtils.deDot(entry.getKey()), entry.getValue());
		}
	}

	public static void setException(Span span, Exception e, Collection<String> ignoredExceptions, Collection<String> unnestExceptions) {
		if (e == null || ignoredExceptions.contains(e.getClass().getName())) {
			return;
		}
		Tags.ERROR.set(span, true);
		Throwable throwable = e;
		if (unnestExceptions.contains(throwable.getClass().getName())) {
			Throwable cause = throwable.getCause();
			if (cause != null) {
				throwable = cause;
			}
		}
		span.setTag("exception.message", throwable.getMessage());
		span.setTag("exception.class", throwable.getClass().getName());

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		span.setTag("exception.stack_trace", sw.getBuffer().toString());
	}

	public static void setCallTree(Span span, CallStackElement callTree) {
		span.setTag(CALL_TREE_JSON, JsonUtils.toJson(callTree));
		span.setTag(CALL_TREE_ASCII, callTree.toString(true));
	}

	public static void setOperationType(Span span, String type) {
		span.setTag("type", type);
	}

	public static void setHttpHeaders(Span span, Map<String, String> headers) {
		if (headers != null) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				span.setTag(HTTP_HEADERS_PREFIX + entry.getKey(), entry.getValue());
			}
		}
	}

	public static com.uber.jaeger.Span getInternalSpan(Span span) {
		return (com.uber.jaeger.Span) span;
	}

	public static boolean isExternalRequest(Span span) {
		com.uber.jaeger.Span jaegerSpan = getInternalSpan(span);
		return jaegerSpan.isRPCClient() &&
				jaegerSpan.getTags().get(ExternalRequestMetricsReporter.EXTERNAL_REQUEST_TYPE) != null &&
				jaegerSpan.getTags().get(ExternalRequestMetricsReporter.EXTERNAL_REQUEST_METHOD) != null;
	}
}
