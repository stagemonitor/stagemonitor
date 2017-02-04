package org.stagemonitor.requestmonitor.anonymization;

import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;
import org.stagemonitor.requestmonitor.utils.IPAnonymizationUtils;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import java.util.concurrent.Callable;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class AnonymizingSpanInterceptor extends SpanInterceptor {

	private final RequestMonitorPlugin requestMonitorPlugin;
	public String username;
	public String ip;
	private final boolean pseudonymizeUserNames;
	private final boolean anonymizeIPs;
	private final boolean active;

	public AnonymizingSpanInterceptor(RequestMonitorPlugin requestMonitorPlugin) {
		this.requestMonitorPlugin = requestMonitorPlugin;
		pseudonymizeUserNames = requestMonitorPlugin.isPseudonymizeUserNames();
		anonymizeIPs = requestMonitorPlugin.isAnonymizeIPs();
		active = anonymizeIPs || pseudonymizeUserNames;
	}


	public static Callable<SpanInterceptor> asCallable(final RequestMonitorPlugin requestMonitorPlugin) {
		return new Callable<SpanInterceptor>() {
			@Override
			public SpanInterceptor call() throws Exception {
				return new AnonymizingSpanInterceptor(requestMonitorPlugin);
			}
		};
	}

	@Override
	public String onSetTag(String key, String value) {
		if (!active) {
			return super.onSetTag(key, value);
		}
		if (SpanUtils.USERNAME.equals(key)) {
			username = value;
		} else if (SpanUtils.IPV4_STRING.equals(key)) {
			ip = value;
		} else if (Tags.PEER_HOST_IPV6.getKey().equals(key)) {
			ip = value;
		}
		return value;
	}

	@Override
	public void onFinish(Span span, String operationName, long durationNanos) {
		if (!active) {
			return;
		}
		String hashedUserName = username;
		if (pseudonymizeUserNames) {
			hashedUserName = StringUtils.sha1Hash(username);
			span.setTag(SpanUtils.USERNAME, hashedUserName);
		}
		final boolean disclose = requestMonitorPlugin.getDiscloseUsers().contains(hashedUserName);
		if (disclose) {
			span.setTag("username_disclosed", username);
		}
		if (anonymizeIPs && ip != null && !disclose) {
			SpanUtils.setClientIp(span, IPAnonymizationUtils.anonymize(ip));
		}
	}
}
