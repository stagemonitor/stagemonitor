package org.stagemonitor.core.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class StringUtils {

	public static final Pattern CAMEL_CASE = Pattern.compile("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])");

	private StringUtils() {
	}

	public static String removeStart(final String str, final String remove) {
		if (remove != null && str.startsWith(remove)) {
			return str.substring(remove.length());
		} else {
			return str;
		}
	}

	public static String capitalize(String self) {
		return Character.toUpperCase(self.charAt(0)) + self.substring(1);
	}

	public static String splitCamelCase(String s) {
		return CAMEL_CASE.matcher(s).replaceAll(" ");
	}

	public static String dateAsIsoString(Date date) {
		TimeZone tz = TimeZone.getDefault();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		df.setTimeZone(tz);
		return df.format(date);
	}

	public static String timestampAsIsoString(long timestamp) {
		return dateAsIsoString(new Date(timestamp));
	}

	public static String slugify(String s) {
		return replaceWhitespacesWithDash(s.toLowerCase().replaceAll("[^\\w ]+", ""));
	}

	public static boolean isNotEmpty(String s) {
		return !isEmpty(s);
	}

	public static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}

	public static String asCsv(String[] strings) {
		return asCsv(Arrays.asList(strings));
	}

	public static String asCsv(Collection<?> values) {
		if (values == null) {
			return null;
		}
		final String s = new ArrayList<Object>(values).toString();
		// removes []
		return s.substring(1, s.length() - 1);
	}

	public static String getLogstashStyleDate() {
		return getLogstashStyleDate(System.currentTimeMillis());
	}

	public static String getLogstashStyleDate(long time) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat.format(new Date(time));
	}

	public static String replaceWhitespacesWithDash(String s) {
		if (s == null) {
			return null;
		}
		return s.replaceAll("\\s", "-");
	}

	public static String toCommaSeparatedString(String... strings) {
		StringBuilder sb = new StringBuilder();
		for (String value : strings) {
			sb.append(value).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static String removeTrailingSlash(String url) {
		if (url != null && url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
