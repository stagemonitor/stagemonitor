package org.stagemonitor.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class StringUtils {

	private static final Pattern CAMEL_CASE = Pattern.compile("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])");
	private static final char[] hexArray = "0123456789abcdef".toCharArray();

	private static final ThreadLocal<DateFormat> LOGSTASH_STYLE_FORMAT = new ThreadLocal<DateFormat>();

	private static final ThreadLocal<DateFormat> ISO_STRING_FORMAT = new ThreadLocal<DateFormat>();

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

	/**
	 * Copied from https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/StringUtils.java
	 *
	 * <p>Splits the provided text into an array, separator specified.
	 * This is an alternative to using StringTokenizer.</p>
	 *
	 * <p>The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * For more control over the split use the StrTokenizer class.</p>
	 *
	 * <p>A {@code null} input String returns {@code null}.</p>
	 *
	 * <pre>
	 * StringUtils.split(null, *)         = null
	 * StringUtils.split("", *)           = []
	 * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
	 * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
	 * StringUtils.split("a:b:c", '.')    = ["a:b:c"]
	 * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
	 * </pre>
	 *
	 * @param str  the String to parse, may be null
	 * @param separatorChar  the character used as the delimiter
	 * @return an array of parsed Strings, {@code null} if null String input
	 * @since 2.0
	 */
	public static String[] split(final String str, final char separatorChar) {
		if (str == null) {
			return null;
		}
		final int len = str.length();
		if (len == 0) {
			return new String[0];
		}
		final List<String> list = new ArrayList<String>();
		int i = 0, start = 0;
		boolean match = false;
		while (i < len) {
			if (str.charAt(i) == separatorChar) {
				if (match) {
					list.add(str.substring(start, i));
					match = false;
				}
				start = ++i;
				continue;
			}
			match = true;
			i++;
		}
		if (match) {
			list.add(str.substring(start, i));
		}
		return list.toArray(new String[list.size()]);
	}

	public static String dateAsIsoString(Date date) {
		DateFormat dateFormat = ISO_STRING_FORMAT.get();
		if (dateFormat == null) {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			dateFormat.setTimeZone(TimeZone.getDefault());
		}
		return dateFormat.format(date);
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
		DateFormat dateFormat = LOGSTASH_STYLE_FORMAT.get();
		if (dateFormat == null) {
			dateFormat = new SimpleDateFormat("yyyy.MM.dd");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			LOGSTASH_STYLE_FORMAT.set(dateFormat);
		}
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
		for (int i = 0; i < strings.length; i++) {
			String value = strings[i];
			if (i > 0) {
				sb.append(",");
			}
			sb.append(value);
		}
		return sb.toString();
	}

	public static String removeTrailingSlash(String url) {
		if (url != null && url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	public static String sha1Hash(String s) {
		if (s == null) {
			return null;
		}
		final MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		final byte[] digest = messageDigest.digest(s.getBytes(Charset.forName("UTF-8")));
		return bytesToHex(digest);
	}

	// kudos to maybeWeCouldStealAVan (http://stackoverflow.com/a/9855338/1125055)
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static String toHexString(long l) {
		return String.format("%x", l);
	}

	public static String deDot(String s) {
		return s.replace(".", "_(dot)_");
	}

	public static String reDot(String s) {
		return s.replace("_(dot)_", ".");
	}
}
