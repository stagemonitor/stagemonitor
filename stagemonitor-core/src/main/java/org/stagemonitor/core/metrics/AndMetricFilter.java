package org.stagemonitor.core.metrics;

import java.util.Arrays;
import java.util.List;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

public class AndMetricFilter implements MetricFilter {
	
	private final List<MetricFilter> metricFilters;

	public AndMetricFilter(List<MetricFilter> metricFilters) {
		this.metricFilters = metricFilters;
	}

	public AndMetricFilter(MetricFilter... metricFilters) {
		this.metricFilters = Arrays.asList(metricFilters);
	}

	@Override
	public boolean matches(String name, Metric metric) {
		for (MetricFilter metricFilter : metricFilters) {
			if (metricFilter != null && !metricFilter.matches(name, metric)) {
				return false;
			}
		}
		return true;
	}

}
