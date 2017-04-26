package org.stagemonitor.tracing.anonymization;

import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.IPAnonymizationUtils;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.AbstractSpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanEventListener;
import org.stagemonitor.tracing.wrapper.SpanEventListenerFactory;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.util.StringUtils;

import io.opentracing.tag.Tags;

public class AnonymizingSpanEventListener extends AbstractSpanEventListener {

	private final TracingPlugin tracingPlugin;
	public String username;
	public String ip;
	private final boolean pseudonymizeUserNames;
	private final boolean anonymizeIPs;
	private final boolean active;

	public AnonymizingSpanEventListener(TracingPlugin tracingPlugin) {
		this.tracingPlugin = tracingPlugin;
		pseudonymizeUserNames = tracingPlugin.isPseudonymizeUserNames();
		anonymizeIPs = tracingPlugin.isAnonymizeIPs();
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
		final boolean disclose = tracingPlugin.getDiscloseUsers().contains(hashedUserName);
		if (disclose) {
			spanWrapper.getDelegate().setTag("username_disclosed", username);
		}
		if (anonymizeIPs && ip != null && !disclose) {
			SpanUtils.setClientIp(spanWrapper, IPAnonymizationUtils.anonymize(ip));
		}
	}

	public static class MySpanEventListenerFactory implements SpanEventListenerFactory {
		private final TracingPlugin tracingPlugin;

		public MySpanEventListenerFactory(TracingPlugin tracingPlugin) {
			this.tracingPlugin = tracingPlugin;
		}

		@Override
		public SpanEventListener create() {
			return new AnonymizingSpanEventListener(tracingPlugin);
		}
	}
}
