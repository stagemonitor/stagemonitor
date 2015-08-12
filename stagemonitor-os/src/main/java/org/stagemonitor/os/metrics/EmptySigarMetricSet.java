package org.stagemonitor.os.metrics;

import java.util.Collections;
import java.util.Map;

import com.codahale.metrics.Metric;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class EmptySigarMetricSet<T> extends AbstractSigarMetricSet<T> {

	public EmptySigarMetricSet() {
		super(null);
	}

	@Override
	T loadSnapshot(Sigar sigar) throws SigarException {
		return null;
	}

	@Override
	public Map<MetricName, Metric> getMetrics() {
		return Collections.emptyMap();
	}
}
