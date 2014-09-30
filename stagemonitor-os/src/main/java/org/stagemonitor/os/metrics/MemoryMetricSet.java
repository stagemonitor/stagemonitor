package org.stagemonitor.os.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.util.HashMap;
import java.util.Map;

public class MemoryMetricSet extends AbstractSigarMetricSet<Mem> {

	public MemoryMetricSet(Sigar sigar) throws SigarException {
		super(sigar);
	}

	@Override
	Mem loadSnapshot(Sigar sigar) throws SigarException {
		return sigar.getMem();
	}

	@Override
	public Map<String, Metric> getMetrics() {
		Map<String, Metric> metrics = new HashMap<String, Metric>();
		metrics.put("os.mem.usage.free", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getFree();
			}
		});
		metrics.put("os.mem.usage.used", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getUsed();
			}
		});
		metrics.put("os.mem.usage.total", new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTotal();
			}
		});
		metrics.put("os.mem.usage-percent", new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getUsedPercent() / 100;
			}
		});
		return metrics;
	}
}
