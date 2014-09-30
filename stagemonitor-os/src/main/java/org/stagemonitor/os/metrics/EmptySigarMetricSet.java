package org.stagemonitor.os.metrics;

import com.codahale.metrics.Metric;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.util.Collections;
import java.util.Map;

public class EmptySigarMetricSet<T> extends AbstractSigarMetricSet<T> {

	public EmptySigarMetricSet() {
		super(null);
	}

	@Override
	T loadSnapshot(Sigar sigar) throws SigarException {
		return null;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		return Collections.emptyMap();
	}
}
