package org.stagemonitor.requestmonitor.anonymization;

import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;
import org.stagemonitor.requestmonitor.utils.IPAnonymizationUtils;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

public class AnonymizingSpanInterceptor extends SpanInterceptor {

	private final RequestMonitorPlugin requestMonitorPlugin;
	public String username;
	public String ip;
	private final boolean pseudonymizeUserNames;
	private final boolean anonymizeIPs;

	public AnonymizingSpanInterceptor(RequestMonitorPlugin requestMonitorPlugin) {
		this.requestMonitorPlugin = requestMonitorPlugin;
		pseudonymizeUserNames = requestMonitorPlugin.isPseudonymizeUserNames();
		anonymizeIPs = requestMonitorPlugin.isAnonymizeIPs();
	}

	@Override
	public String onSetTag(String key, String value) {
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
	public void onFinish(Span span, long endTimestampNanos) {
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
