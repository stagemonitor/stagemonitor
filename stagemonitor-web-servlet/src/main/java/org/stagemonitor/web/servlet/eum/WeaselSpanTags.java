package org.stagemonitor.web.servlet.eum;

import java.util.HashMap;
import java.util.Map;

public class WeaselSpanTags {

	public static final String TIMING_UNLOAD = "timing.unload";
	public static final String TIMING_REDIRECT = "timing.redirect";
	public static final String TIMING_APP_CACHE_LOOKUP = "timing.app_cache_lookup";
	public static final String TIMING_DNS_LOOKUP = "timing.dns_lookup";
	public static final String TIMING_TCP = "timing.tcp";
	public static final String TIMING_SSL = "timing.ssl";
	public static final String TIMING_REQUEST = "timing.request";
	public static final String TIMING_RESPONSE = "timing.response";
	public static final String TIMING_PROCESSING = "timing.processing";
	public static final String TIMING_LOAD = "timing.load";
	public static final String TIMING_TIME_TO_FIRST_PAINT = "timing.time_to_first_paint";
	public static final String TIMING_RESOURCE = "timing.resource";

	private static final Map<String, String> spanTagToWeaselRequestParameterName = new HashMap<String, String>();

	static {
		addMapping(TIMING_UNLOAD, "t_unl");
		addMapping(TIMING_REDIRECT, "t_red");
		addMapping(TIMING_APP_CACHE_LOOKUP, "t_apc");
		addMapping(TIMING_DNS_LOOKUP, "t_dns");
		addMapping(TIMING_TCP, "t_tcp");
		addMapping(TIMING_SSL, "t_ssl");
		addMapping(TIMING_REQUEST, "t_req");
		addMapping(TIMING_RESPONSE, "t_rsp");
		addMapping(TIMING_PROCESSING, "t_pro");
		addMapping(TIMING_LOAD, "t_loa");
		addMapping(TIMING_TIME_TO_FIRST_PAINT, "t_fp");
	}

	private static void addMapping(String spanTag, String weaselRequestParameterName) {
		spanTagToWeaselRequestParameterName.put(spanTag, weaselRequestParameterName);
	}

	public static String getWeaselRequestParameterName(String tag) {
		final String weaselRequestParameterName = spanTagToWeaselRequestParameterName.get(tag);
		if (weaselRequestParameterName == null) {
			throw new IllegalArgumentException("no weasel request parameter exists for timing tag " + tag);
		}
		return weaselRequestParameterName;
	}

}
