package de.isys.jawap.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class GraphiteEncoder {

	public static final String UTF_8 = "UTF-8";

	public static String encodeForGraphite(String s) {
		try {
			return URLEncoder.encode(s.replace(".", ":dot:"), UTF_8).replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String decodeForGraphite(String s) {
		try {
			return URLDecoder.decode(s, UTF_8).replace(":dot:", ".");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
