package org.stagemonitor.core.metrics.metrics2;

import java.util.Arrays;
import java.util.List;

import com.codahale.metrics.Metric;

public class AndMetric2Filter implements Metric2Filter {

	private final List<Metric2Filter> metricFilters;

	public AndMetric2Filter(List<Metric2Filter> metricFilters) {
		this.metricFilters = metricFilters;
	}

	public AndMetric2Filter(Metric2Filter... metricFilters) {
		this.metricFilters = Arrays.asList(metricFilters);
	}

	@Override
	public boolean matches(MetricName name, Metric metric) {
		for (Metric2Filter metricFilter : metricFilters) {
			if (metricFilter != null && !metricFilter.matches(name, metric)) {
				return false;
			}
		}
		return true;
	}

}