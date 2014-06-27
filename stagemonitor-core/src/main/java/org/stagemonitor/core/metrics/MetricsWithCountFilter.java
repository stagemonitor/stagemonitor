package org.stagemonitor.core.metrics;

import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

public class MetricsWithCountFilter implements MetricFilter {
	@Override
	public boolean matches(String name, Metric metric) {
		if (metric instanceof Metered) {
			return ((Metered) metric).getCount() > 0;
		}
		return true;
	}
}
