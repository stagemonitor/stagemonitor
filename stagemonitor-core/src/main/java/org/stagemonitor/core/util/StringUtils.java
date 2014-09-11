package org.stagemonitor.core.util;

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
}
