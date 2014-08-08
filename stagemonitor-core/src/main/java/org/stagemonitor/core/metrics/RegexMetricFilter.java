package org.stagemonitor.core.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import java.util.Collection;
import java.util.regex.Pattern;

public class RegexMetricFilter implements MetricFilter {

	private final Collection<Pattern> patterns;

	public RegexMetricFilter(Collection<Pattern> patterns) {
		this.patterns = patterns;
	}

	@Override
	public boolean matches(String name, Metric metric) {
		for (final Pattern pattern : patterns) {
			if (pattern.matcher(name).matches()) {
				return true;
			}
		}
		return false;
	}
}
