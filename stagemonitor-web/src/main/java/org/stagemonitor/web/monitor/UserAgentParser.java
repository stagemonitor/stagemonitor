package org.stagemonitor.web.monitor;

import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import io.opentracing.Span;

/**
 * The uadetector library is discontinued as the underlying database is now commercial.
 * <p/>
 * Consider using the Elasticsearch ingest user agent plugin: https://www.elastic.co/guide/en/elasticsearch/plugins/master/ingest-user-agent.html
 */
@Deprecated
public class UserAgentParser {

	private static final int MAX_ELEMENTS = 100;

	// prevents reDOS attacks like described in https://github.com/before/uadetector/issues/130
	private static final int MAX_USERAGENT_LENGTH = 256;
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
		if (userAgentHeader != null && userAgentHeader.length() < MAX_USERAGENT_LENGTH) {
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

	private ReadableUserAgent parse(String userAgentHeader) {
		ReadableUserAgent readableUserAgent = userAgentCache.get(userAgentHeader);
		if (readableUserAgent == null) {
			readableUserAgent = parser.parse(userAgentHeader);
			userAgentCache.put(userAgentHeader, readableUserAgent);
		}
		return readableUserAgent;
	}

}
