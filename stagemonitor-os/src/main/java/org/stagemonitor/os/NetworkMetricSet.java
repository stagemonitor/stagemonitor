package org.stagemonitor.os;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Tcp;

import java.util.HashMap;
import java.util.Map;

public class NetworkMetricSet implements MetricSet {

	private final Tcp tcp;

	public NetworkMetricSet(Tcp tcp) {
		this.tcp = tcp;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		return metrics;
	}
}
