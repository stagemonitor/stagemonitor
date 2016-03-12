package org.stagemonitor.core.metrics;

import java.util.Arrays;
import java.util.Collection;

import com.codahale.metrics.Metric;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class MetricNameFilter implements Metric2Filter {
	
	/**
	 * A {@link Metric2Filter} that excludes any results
	 * that match any of the provided patterns.
	 * @param patterns
	 * @return
	 */
	public static MetricNameFilter excludePatterns(MetricName... patterns) {
		return new MetricNameFilter(Arrays.asList(patterns), false);
	}
	/**
	 * A {@link Metric2Filter} that excludes any results
	 * that match any of the provided patterns.
	 * @param patterns
	 * @return
	 */
	public static MetricNameFilter excludePatterns(Collection<MetricName> patterns) {
		return new MetricNameFilter(patterns, false);
	}
	
	/**
	 * A {@link Metric2Filter} that matches (includes) any results
	 * that match any of the provided patterns.
	 * @param patterns
	 * @return
	 */
	public static MetricNameFilter includePatterns(MetricName... patterns) {
		return new MetricNameFilter(Arrays.asList(patterns), true);
	}
	
	private final Collection<MetricName> patterns;
	private final boolean matchResult;
	
	/**
	 * Equivalent to {@link #includePatterns(MetricName...)}
	 * @param patterns
	 */
	public MetricNameFilter(Collection<MetricName> patterns) {
		this(patterns, true);
	}

	private MetricNameFilter(Collection<MetricName> patterns, boolean matchResult) {
		this.patterns = patterns;
		this.matchResult = matchResult;
	}

	@Override
	public boolean matches(MetricName name, Metric metric) {
		for (final MetricName pattern : patterns) {
			if (name.matches(pattern)) {
				return matchResult;
			}
		}
		return !matchResult;
	}
}
