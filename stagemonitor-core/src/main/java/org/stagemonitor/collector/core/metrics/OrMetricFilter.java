package org.stagemonitor.collector.core.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

import java.util.Arrays;
import java.util.List;

public class OrMetricFilter implements MetricFilter {

	private final List<MetricFilter> metricFilters;

	public OrMetricFilter(List<MetricFilter> metricFilters) {
		this.metricFilters = metricFilters;
	}

	public OrMetricFilter(MetricFilter... metricFilters) {
		this.metricFilters = Arrays.asList(metricFilters);
	}

	@Override
	public boolean matches(String name, Metric metric) {
		for (MetricFilter metricFilter : metricFilters) {
			if (metricFilter != null && metricFilter.matches(name, metric)) {
				return true;
			}
		}
		return false;
	}
}
