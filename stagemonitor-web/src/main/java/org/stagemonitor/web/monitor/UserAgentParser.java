package org.stagemonitor.web.monitor;

import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;

import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.web.WebPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

import io.opentracing.Span;

public class UserAgentParser {

	private static final int MAX_ELEMENTS = 100;
	private final UserAgentStringParser parser;
	private final Map<String, ReadableUserAgent> userAgentCache;

	public UserAgentParser() {
		this(UADetectorServiceFactory.getResourceModuleParser(),
				new LinkedHashMap<String, ReadableUserAgent>(MAX_ELEMENTS + 1, 0.75f, true) {
					@Override
					protected boolean removeEldestEntry(Map.Entry eldest) {
						return size() > MAX_ELEMENTS;
					}
				});
	}

	public UserAgentParser(UserAgentStringParser parser, Map<String, ReadableUserAgent> userAgentCache) {
		this.parser = parser;
		this.userAgentCache = userAgentCache;
	}

	public void setUserAgentInformation(final Span span, final String userAgentHeader) {
		if (Stagemonitor.getPlugin(WebPlugin.class).isParseUserAgent()) {
			if (userAgentHeader != null) {
				final ReadableUserAgent userAgent = parse(userAgentHeader);
				span.setTag("user_agent.type", userAgent.getTypeName());
				span.setTag("user_agent.device", userAgent.getDeviceCategory().getName());
				span.setTag("user_agent.os", userAgent.getOperatingSystem().getName());
				span.setTag("user_agent.os_family", userAgent.getOperatingSystem().getFamilyName());
				span.setTag("user_agent.os_version", userAgent.getOperatingSystem().getVersionNumber().toVersionString());
				span.setTag("user_agent.browser", userAgent.getName());
				span.setTag("user_agent.browser_version", userAgent.getVersionNumber().toVersionString());
			}
		}
	}

	private ReadableUserAgent parse(String userAgentHeader) {
		ReadableUserAgent readableUserAgent = userAgentCache.get(userAgentHeader);
		if (readableUserAgent == null) {
			readableUserAgent = parser.parse(userAgentHeader);
			userAgentCache.put(userAgentHeader, readableUserAgent);
		}
		return readableUserAgent;
	}

}
