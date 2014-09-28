package org.stagemonitor.os;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import org.hyperic.sigar.Mem;

import java.util.HashMap;
import java.util.Map;

public class MemoryMetricSet implements MetricSet {

	private final Mem mem;

	public MemoryMetricSet(Mem mem) {
		this.mem = mem;
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put("os.mem.usage.free", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return mem.getFree();
			}
		});
		metrics.put("os.mem.usage.used", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return mem.getUsed();
			}
		});
		metrics.put("os.mem.usage.total", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return mem.getTotal();
			}
		});
		return metrics;
	}
}
