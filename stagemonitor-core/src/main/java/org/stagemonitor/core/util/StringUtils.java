package org.stagemonitor.core.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class StringUtils {

	public static final Pattern CAMEL_CASE = Pattern.compile("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])");

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
		return s.toLowerCase().replaceAll("[^\\w ]+", "").replaceAll("\\s+", "-");
	}

	public static boolean isNotEmpty(String s) {
		return !isEmpty(s);
	}

	public static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}
}
