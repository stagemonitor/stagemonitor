package org.stagemonitor.core.metrics;

import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import org.stagemonitor.core.metrics.metrics2.Metric2Filter;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class MetricsWithCountFilter implements Metric2Filter {
	@Override
	public boolean matches(MetricName name, Metric metric) {
		if (metric instanceof Metered) {
			return ((Metered) metric).getCount() > 0;
		}
		return true;
	}
}
