package org.stagemonitor.requestmonitor.anonymization;

import org.stagemonitor.core.util.StringUtils;
import org.stagemonitor.requestmonitor.RequestMonitorPlugin;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptor;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanInterceptorFactory;
import org.stagemonitor.requestmonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.requestmonitor.utils.IPAnonymizationUtils;
import org.stagemonitor.requestmonitor.utils.SpanUtils;

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
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		if (!active) {
			return;
		}
		String hashedUserName = username;
		if (pseudonymizeUserNames) {
			hashedUserName = StringUtils.sha1Hash(username);
			spanWrapper.getDelegate().setTag(SpanUtils.USERNAME, hashedUserName);
		}
		final boolean disclose = requestMonitorPlugin.getDiscloseUsers().contains(hashedUserName);
		if (disclose) {
			spanWrapper.getDelegate().setTag("username_disclosed", username);
		}
		if (anonymizeIPs && ip != null && !disclose) {
			SpanUtils.setClientIp(spanWrapper, IPAnonymizationUtils.anonymize(ip));
		}
	}

	public static class MySpanInterceptorFactory implements SpanInterceptorFactory {
		private final RequestMonitorPlugin requestMonitorPlugin;

		public MySpanInterceptorFactory(RequestMonitorPlugin requestMonitorPlugin) {
			this.requestMonitorPlugin = requestMonitorPlugin;
		}

		@Override
		public SpanInterceptor create() {
			return new AnonymizingSpanInterceptor(requestMonitorPlugin);
		}
	}
}
