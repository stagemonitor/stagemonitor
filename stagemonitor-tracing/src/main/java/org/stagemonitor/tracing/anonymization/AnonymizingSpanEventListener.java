package org.stagemonitor.tracing.anonymization;

import org.stagemonitor.core.util.InetAddresses;
import org.stagemonitor.tracing.TracingPlugin;
import org.stagemonitor.tracing.utils.IPAnonymizationUtils;
import org.stagemonitor.tracing.utils.SpanUtils;
import org.stagemonitor.tracing.wrapper.SpanWrapper;
import org.stagemonitor.tracing.wrapper.StatelessSpanEventListener;
import org.stagemonitor.util.StringUtils;

import java.net.Inet4Address;

import io.opentracing.tag.Tags;

public class AnonymizingSpanEventListener extends StatelessSpanEventListener {

	private final TracingPlugin tracingPlugin;

	public AnonymizingSpanEventListener(TracingPlugin tracingPlugin) {
		this.tracingPlugin = tracingPlugin;
	}

	@Override
	public void onFinish(SpanWrapper spanWrapper, String operationName, long durationNanos) {
		final boolean pseudonymizeUserNames = tracingPlugin.isPseudonymizeUserNames();
		final boolean anonymizeIPs = tracingPlugin.isAnonymizeIPs();
		if (!anonymizeIPs && !pseudonymizeUserNames) {
			return;
		}
		final String username = spanWrapper.getStringTag(SpanUtils.USERNAME);
		String hashedUserName = username;
		if (pseudonymizeUserNames) {
			hashedUserName = StringUtils.sha1Hash(username);
			spanWrapper.setTag(SpanUtils.USERNAME, hashedUserName);
		}
		final boolean disclose = tracingPlugin.getDiscloseUsers().contains(hashedUserName);
		if (disclose) {
			spanWrapper.setTag("username_disclosed", username);
		}
		if (anonymizeIPs && !disclose) {
			final String ipV6Address = spanWrapper.getStringTag(Tags.PEER_HOST_IPV6.getKey());
			final Number ipV4Address = spanWrapper.getNumberTag(Tags.PEER_HOST_IPV4.getKey());
			if (ipV6Address != null) {
				Tags.PEER_HOST_IPV6.set(spanWrapper, IPAnonymizationUtils.anonymize(ipV6Address));
			} else if (ipV4Address != null) {
				final Inet4Address anonymizedIp = IPAnonymizationUtils.anonymizeIpV4Address(InetAddresses.fromInteger(ipV4Address.intValue()));
				Tags.PEER_HOST_IPV4.set(spanWrapper, InetAddresses.inetAddressToInt(anonymizedIp));
			}
		}
	}

}
