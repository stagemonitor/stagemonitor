package org.stagemonitor.core.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import java.util.Collection;
import java.util.regex.Pattern;

public class RegexMetricFilter implements MetricFilter {
	
	/**
	 * A {@link MetricFilter} that excludes any results
	 * that match any of the provided patterns.
	 * @param patterns
	 * @return
	 */
	public static RegexMetricFilter excludePatterns(Collection<Pattern> patterns) {
		return new RegexMetricFilter(patterns, false);
	}
	
	/**
	 * A {@link MetricFilter} that matches (includes) any results
	 * that match any of the provided patterns.
	 * @param patterns
	 * @return
	 */
	public static RegexMetricFilter includePatterns(Collection<Pattern> patterns) {
		return new RegexMetricFilter(patterns, true);
	}
	
	private final Collection<Pattern> patterns;
	private final boolean matchResult;
	
	/**
	 * Equivalent to {@link #includePatterns(Collection)}
	 * @param patterns
	 */
	public RegexMetricFilter(Collection<Pattern> patterns) {
		this(patterns, true);
	}

	private RegexMetricFilter(Collection<Pattern> patterns, boolean matchResult) {
		this.patterns = patterns;
		this.matchResult = matchResult;
	}

	@Override
	public boolean matches(String name, Metric metric) {
		for (final Pattern pattern : patterns) {
			if (pattern.matcher(name).matches()) {
				return matchResult;
			}
		}
		return !matchResult;
	}
}
