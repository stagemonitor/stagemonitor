package org.stagemonitor.collector.core.util;

import java.util.regex.Pattern;

public class GraphiteSanitizer {
	public static final Pattern DISALLOWED_CHARS = Pattern.compile("[^a-zA-Z0-9!#\\$%&\"'\\*\\+\\-:;<=>\\?@\\[\\\\\\]\\^_`\\|~]");

	/**
	 * Graphite only supports alphanumeric characters + !#$%&"'*+-.:;<=>?@[\]^_`|~ so each metric name segment has to
	 * be cleared from other chars.
	 * <p/>
	 * <pre>
	 * metric path/segment delimiter
	 *         _|_
	 *        |   |
	 * metrics.cpu.utilisation  <- metric name
	 *    |     |      |
	 *    -------------
	 *          |
	 *  metric name segments
	 * </pre>
	 *
	 * @param metricNameSegment the metric name segment (see diagram above for a explanation of what a metric name segment is)
	 * @return the metricNameSegment that contains only characters that graphite can handle
	 */
	public static String sanitizeGraphiteMetricSegment(String metricNameSegment) {
		return DISALLOWED_CHARS.matcher(metricNameSegment.replace('.', ':').replace(' ', '-').replace('/', '|')).replaceAll("_");
	}
}
