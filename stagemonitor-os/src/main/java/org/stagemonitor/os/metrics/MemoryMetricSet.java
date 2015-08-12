package org.stagemonitor.os.metrics;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class MemoryMetricSet extends AbstractSigarMetricSet<Mem> {

	public MemoryMetricSet(Sigar sigar) throws SigarException {
		super(sigar);
	}

	@Override
	Mem loadSnapshot(Sigar sigar) throws SigarException {
		return sigar.getMem();
	}

	@Override
	public Map<MetricName, Metric> getMetrics() {
		Map<MetricName, Metric> metrics = new HashMap<MetricName, Metric>();
		metrics.put(name("mem_usage").type("free").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getFree();
			}
		});
		metrics.put(name("mem_usage").type("used").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getUsed();
			}
		});
		metrics.put(name("mem_usage").type("total").build(), new Gauge<Long>() {
			@Override
			public Long getValue() {
				return getSnapshot().getTotal();
			}
		});
		metrics.put(name("mem_usage_percent").build(), new Gauge<Double>() {
			@Override
			public Double getValue() {
				return getSnapshot().getUsedPercent() / 100;
			}
		});
		return metrics;
	}
}
