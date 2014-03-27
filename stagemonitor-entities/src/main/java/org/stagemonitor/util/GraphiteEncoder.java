package org.stagemonitor.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class GraphiteEncoder {

	public static final String UTF_8 = "UTF-8";

	public static String encodeForGraphite(String s) {
		try {
			return URLEncoder.encode(s, UTF_8)
					// manually encode '.' as graphite treats it as delimiter
					.replace(".", "%2e")
					// force encoding of ' ' to '%20' rather than '+' to ensure symmetrical behaviour to JavaScript's encodeURLComponent
					.replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String decodeForGraphite(String s) {
		try {
			return URLDecoder.decode(s, UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
